package cc.hrva.ladica.service;

import cc.hrva.ladica.configuration.properties.AppProperties;
import cc.hrva.ladica.dto.LinkDto;
import cc.hrva.ladica.exception.LinkLimitExceededException;
import cc.hrva.ladica.model.LadicaLink;
import cc.hrva.ladica.model.UserAccount;
import cc.hrva.ladica.repository.LadicaLinkRepository;
import cc.hrva.ladica.repository.UserAccountRepository;
import cc.hrva.ladica.service.impl.DefaultLinkSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultLinkSyncServiceTest {

    @Mock
    private LadicaLinkRepository linkRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private DefaultLinkSyncService service;

    private final UserAccount user = UserAccount.builder().id(1L).email("a@b.com").build();

    @Test
    void pushInsertsNewLinkAndAssignsSeq() {
        givenLockedUserWithCount(0L);
        when(linkRepository.findByUserAndClientId(user, "c1")).thenReturn(Optional.empty());
        when(linkRepository.findByUserAndNormalizedUrlAndDeletedFalse(eq(user), any())).thenReturn(Optional.empty());
        when(appProperties.getMaxLinksPerUser()).thenReturn(5000L);
        when(linkRepository.findTopByUserOrderByServerSeqDesc(user))
                .thenReturn(Optional.empty(), Optional.of(linkWithSeq(1L)));
        when(linkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = service.push(user, List.of(new LinkDto("c1", "konzum.hr", "Konzum", "shop", 100L, false)));

        assertThat(response.cursor()).isEqualTo(1L);
        verify(linkRepository).save(argThat(savedLink ->
                savedLink.getServerSeq() == 1L && "konzum.hr".equals(savedLink.getNormalizedUrl())));
    }

    @Test
    void pushIgnoresOlderUpdate() {
        givenLockedUserWithCount(0L);
        final var storedLink = LadicaLink.builder().id(10L).user(user).clientId("c1").updatedAt(200L).serverSeq(5L).build();
        when(linkRepository.findByUserAndClientId(user, "c1")).thenReturn(Optional.of(storedLink));
        when(linkRepository.findTopByUserOrderByServerSeqDesc(user)).thenReturn(Optional.of(linkWithSeq(5L)));

        final var response = service.push(user, List.of(new LinkDto("c1", "x.com", "X", "other", 100L, false)));

        verify(linkRepository, never()).save(any());
        assertThat(response.cursor()).isEqualTo(5L);
    }

    @Test
    void pushAppliesNewerUpdateAndTombstone() {
        givenLockedUserWithCount(0L);
        final var storedLink = LadicaLink.builder().id(10L).user(user).clientId("c1").updatedAt(100L).serverSeq(5L).build();
        when(linkRepository.findByUserAndClientId(user, "c1")).thenReturn(Optional.of(storedLink));
        when(linkRepository.findTopByUserOrderByServerSeqDesc(user))
                .thenReturn(Optional.of(linkWithSeq(5L)), Optional.of(linkWithSeq(6L)));
        when(linkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = service.push(user, List.of(new LinkDto("c1", "x.com", "X", "other", 300L, true)));

        verify(linkRepository).save(argThat(savedLink ->
                savedLink.isDeleted() && savedLink.getServerSeq() == 6L && savedLink.getUpdatedAt() == 300L));
        assertThat(response.cursor()).isEqualTo(6L);
    }

    @Test
    void pushDeduplicatesOnUpdateByTombstoningTheExistingDuplicate() {
        givenLockedUserWithCount(0L);
        final var storedLink = LadicaLink.builder()
                .id(10L).user(user).clientId("c1").url("old.com").normalizedUrl("old.com").updatedAt(100L).serverSeq(3L).build();
        final var existingDuplicate = LadicaLink.builder()
                .id(20L).user(user).clientId("c2").url("dup.com").normalizedUrl("dup.com").updatedAt(200L).serverSeq(4L).build();
        when(linkRepository.findByUserAndClientId(user, "c1")).thenReturn(Optional.of(storedLink));
        when(linkRepository.findByUserAndNormalizedUrlAndDeletedFalse(user, "dup.com"))
                .thenReturn(Optional.of(existingDuplicate));
        when(linkRepository.findTopByUserOrderByServerSeqDesc(user))
                .thenReturn(Optional.of(linkWithSeq(4L)), Optional.of(linkWithSeq(5L)), Optional.of(linkWithSeq(6L)));
        when(linkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = service.push(user, List.of(new LinkDto("c1", "https://dup.com", "X", "other", 300L, false)));

        assertThat(existingDuplicate.isDeleted()).isTrue();
        assertThat(existingDuplicate.getServerSeq()).isEqualTo(5L);
        assertThat(storedLink.isDeleted()).isFalse();
        assertThat(storedLink.getNormalizedUrl()).isEqualTo("dup.com");
        assertThat(storedLink.getServerSeq()).isEqualTo(6L);
        assertThat(response.cursor()).isEqualTo(6L);
        verify(linkRepository).save(existingDuplicate);
        verify(linkRepository).save(storedLink);
    }

    @Test
    void pushRejectsNewLinkOverTheUserCeiling() {
        givenLockedUserWithCount(2L);
        when(linkRepository.findByUserAndClientId(user, "c9")).thenReturn(Optional.empty());
        when(linkRepository.findByUserAndNormalizedUrlAndDeletedFalse(user, "new.com")).thenReturn(Optional.empty());
        when(appProperties.getMaxLinksPerUser()).thenReturn(2L);

        final var links = List.of(new LinkDto("c9", "new.com", "New", "other", 100L, false));

        assertThatThrownBy(() -> service.push(user, links)).isInstanceOf(LinkLimitExceededException.class);
        verify(linkRepository, never()).save(any());
    }

    @Test
    void pushReturnsCurrentMaxCursorWhenBatchOnlyContainsIgnoredLinks() {
        givenLockedUserWithCount(0L);
        final var storedLink = LadicaLink.builder().id(10L).user(user).clientId("c1").updatedAt(500L).serverSeq(2L).build();
        when(linkRepository.findByUserAndClientId(user, "c1")).thenReturn(Optional.of(storedLink));
        when(linkRepository.findTopByUserOrderByServerSeqDesc(user)).thenReturn(Optional.of(linkWithSeq(9L)));

        final var response = service.push(user, List.of(new LinkDto("c1", "x.com", "X", "other", 100L, false)));

        verify(linkRepository, never()).save(any());
        assertThat(response.cursor()).isEqualTo(9L);
    }

    @Test
    void pullReturnsChangesAndCursor() {
        final var changedLink = LadicaLink.builder()
                .clientId("c1")
                .url("x.com")
                .normalizedUrl("x.com")
                .updatedAt(1L)
                .serverSeq(7L)
                .build();
        when(linkRepository.findByUserAndServerSeqGreaterThanOrderByServerSeqAsc(user, 3L))
                .thenReturn(List.of(changedLink));

        final var response = service.pull(user, 3L);

        assertThat(response.cursor()).isEqualTo(7L);
        assertThat(response.links()).singleElement()
                .satisfies(linkDto -> assertThat(linkDto.clientId()).isEqualTo("c1"));
    }

    private void givenLockedUserWithCount(final long nonDeletedCount) {
        when(userAccountRepository.findWithLockById(user.getId())).thenReturn(Optional.of(user));
        when(linkRepository.countByUserAndDeletedFalse(user)).thenReturn(nonDeletedCount);
    }

    private LadicaLink linkWithSeq(final long serverSeq) {
        return LadicaLink.builder().serverSeq(serverSeq).build();
    }

}
