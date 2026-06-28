package cc.hrva.ladica.service.impl;

import cc.hrva.ladica.configuration.properties.AppProperties;
import cc.hrva.ladica.dto.LinkDto;
import cc.hrva.ladica.dto.PullResponse;
import cc.hrva.ladica.dto.PushResponse;
import cc.hrva.ladica.exception.LinkLimitExceededException;
import cc.hrva.ladica.exception.NotFoundException;
import cc.hrva.ladica.model.LadicaLink;
import cc.hrva.ladica.model.UserAccount;
import cc.hrva.ladica.repository.LadicaLinkRepository;
import cc.hrva.ladica.repository.UserAccountRepository;
import cc.hrva.ladica.service.LinkSyncService;
import cc.hrva.ladica.util.UrlNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultLinkSyncService implements LinkSyncService {

    private final LadicaLinkRepository linkRepository;
    private final UserAccountRepository userAccountRepository;
    private final AppProperties appProperties;

    @Override
    public PullResponse pull(final UserAccount user, final long since) {
        final var changedLinks = linkRepository.findByUserAndServerSeqGreaterThanOrderByServerSeqAsc(user, since);
        final var links = changedLinks.stream()
                .map(LinkDto::from)
                .toList();
        final var cursor = CollectionUtils.isEmpty(changedLinks)
                ? since
                : changedLinks.getLast().getServerSeq();

        return new PullResponse(links, cursor);
    }

    @Override
    @Transactional
    public PushResponse push(final UserAccount user, final List<LinkDto> links) {
        final var lockedUser = lockUser(user);
        final var nonDeletedCount = new AtomicLong(linkRepository.countByUserAndDeletedFalse(lockedUser));

        for (final var incomingLink : links) {
            applyIncomingLink(lockedUser, incomingLink, nonDeletedCount);
        }

        return new PushResponse(currentMaxSeq(lockedUser));
    }

    private UserAccount lockUser(final UserAccount user) {
        return userAccountRepository.findWithLockById(user.getId())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    private void applyIncomingLink(final UserAccount user, final LinkDto incomingLink, final AtomicLong nonDeletedCount) {
        linkRepository.findByUserAndClientId(user, incomingLink.clientId())
                .ifPresentOrElse(
                        storedLink -> applyToExisting(user, storedLink, incomingLink),
                        () -> insertNewLink(user, incomingLink, nonDeletedCount));
    }

    private void applyToExisting(final UserAccount user, final LadicaLink storedLink, final LinkDto incomingLink) {
        if (incomingLink.updatedAt() < storedLink.getUpdatedAt()) {
            return;
        }

        final var normalizedUrl = UrlNormalizer.normalize(incomingLink.url());
        final var losesDuplicateBattle = !incomingLink.deleted()
                && resolveDuplicate(user, storedLink, normalizedUrl, incomingLink.updatedAt());

        applyFields(storedLink, incomingLink, normalizedUrl);
        if (losesDuplicateBattle) {
            storedLink.setDeleted(true);
        }
        storedLink.setServerSeq(nextSeq(user));
        linkRepository.save(storedLink);
    }

    private void insertNewLink(final UserAccount user, final LinkDto incomingLink, final AtomicLong nonDeletedCount) {
        final var newLink = buildLink(user, incomingLink);
        if (!newLink.isDeleted() && resolveDuplicate(user, newLink, newLink.getNormalizedUrl(), newLink.getUpdatedAt())) {
            newLink.setDeleted(true);
        }
        if (!newLink.isDeleted()) {
            enforceLinkCeiling(nonDeletedCount);
        }
        newLink.setServerSeq(nextSeq(user));
        linkRepository.save(newLink);
    }

    private boolean resolveDuplicate(final UserAccount user, final LadicaLink appliedLink, final String normalizedUrl, final long updatedAt) {
        return linkRepository.findByUserAndNormalizedUrlAndDeletedFalse(user, normalizedUrl)
                .filter(existingDuplicate -> !isSameLink(existingDuplicate, appliedLink))
                .map(existingDuplicate -> tombstoneLoser(user, existingDuplicate, updatedAt))
                .orElse(false);
    }

    private boolean tombstoneLoser(final UserAccount user, final LadicaLink existingDuplicate, final long updatedAt) {
        if (updatedAt < existingDuplicate.getUpdatedAt()) {
            return true;
        }

        existingDuplicate.setDeleted(true);
        existingDuplicate.setServerSeq(nextSeq(user));
        linkRepository.save(existingDuplicate);

        return false;
    }

    private void enforceLinkCeiling(final AtomicLong nonDeletedCount) {
        if (nonDeletedCount.getAndIncrement() >= appProperties.getMaxLinksPerUser()) {
            throw new LinkLimitExceededException(
                    "Link limit of %d reached".formatted(appProperties.getMaxLinksPerUser()));
        }
    }

    private LadicaLink buildLink(final UserAccount user, final LinkDto incomingLink) {
        return LadicaLink.builder()
                .user(user)
                .clientId(incomingLink.clientId())
                .url(incomingLink.url())
                .normalizedUrl(UrlNormalizer.normalize(incomingLink.url()))
                .name(incomingLink.name())
                .cat(incomingLink.cat())
                .updatedAt(incomingLink.updatedAt())
                .deleted(incomingLink.deleted())
                .build();
    }

    private void applyFields(final LadicaLink storedLink, final LinkDto incomingLink, final String normalizedUrl) {
        storedLink.setUrl(incomingLink.url());
        storedLink.setNormalizedUrl(normalizedUrl);
        storedLink.setName(incomingLink.name());
        storedLink.setCat(incomingLink.cat());
        storedLink.setUpdatedAt(incomingLink.updatedAt());
        storedLink.setDeleted(incomingLink.deleted());
    }

    private long currentMaxSeq(final UserAccount user) {
        return linkRepository.findTopByUserOrderByServerSeqDesc(user)
                .map(LadicaLink::getServerSeq)
                .orElse(0L);
    }

    private long nextSeq(final UserAccount user) {
        return linkRepository.findTopByUserOrderByServerSeqDesc(user)
                .map(LadicaLink::getServerSeq)
                .orElse(0L) + 1;
    }

    private boolean isSameLink(final LadicaLink first, final LadicaLink second) {
        return first.getId() != null && first.getId().equals(second.getId());
    }

}
