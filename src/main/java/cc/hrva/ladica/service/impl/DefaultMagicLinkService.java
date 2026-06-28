package cc.hrva.ladica.service.impl;

import cc.hrva.ladica.configuration.properties.AppProperties;
import cc.hrva.ladica.exception.InvalidTokenException;
import cc.hrva.ladica.model.LoginToken;
import cc.hrva.ladica.model.UserAccount;
import cc.hrva.ladica.model.enums.AuthProvider;
import cc.hrva.ladica.repository.LoginTokenRepository;
import cc.hrva.ladica.repository.UserAccountRepository;
import cc.hrva.ladica.security.TokenProvider;
import cc.hrva.ladica.service.MagicLinkService;
import cc.hrva.ladica.service.MailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultMagicLinkService implements MagicLinkService {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String VERIFY_PATH = "/api/v1/auth/magic-link/verify";
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final LoginTokenRepository loginTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final TokenProvider tokenProvider;
    private final MailSender mailSender;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void requestLink(final String email) {
        final var rawToken = generateRawToken();
        final var loginToken = LoginToken.builder()
                .email(email)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(appProperties.getMagicLinkTtlSeconds()))
                .build();
        loginTokenRepository.save(loginToken);

        mailSender.sendMagicLink(email, buildVerifyUrl(rawToken));
        log.debug("Magic link issued for {}", email);
    }

    @Override
    @Transactional
    public String verify(final String rawToken) {
        final var loginToken = loginTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Magic link token not found"));

        rejectUnusableToken(loginToken);

        loginToken.setUsedAt(LocalDateTime.now());
        loginTokenRepository.save(loginToken);

        final var userAccount = findOrCreateUser(loginToken.getEmail());
        userAccount.setLastLoginAt(LocalDateTime.now());
        userAccountRepository.save(userAccount);

        log.debug("Magic link verified for {}", userAccount.getEmail());
        return tokenProvider.createToken(userAccount.getEmail(), List.of(new SimpleGrantedAuthority(ROLE_USER)));
    }

    public static String hash(final String rawToken) {
        try {
            final var digest = MessageDigest.getInstance(HASH_ALGORITHM);
            final var hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashedBytes);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private void rejectUnusableToken(final LoginToken loginToken) {
        if (loginToken.getUsedAt() != null) {
            throw new InvalidTokenException("Magic link token already used");
        }

        if (loginToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Magic link token expired");
        }
    }

    private UserAccount findOrCreateUser(final String email) {
        return userAccountRepository.findByEmail(email)
                .orElseGet(() -> createEmailUser(email));
    }

    private UserAccount createEmailUser(final String email) {
        log.debug("Creating email user {}", email);

        return UserAccount.builder()
                .email(email)
                .authProvider(AuthProvider.EMAIL)
                .active(true)
                .build();
    }

    private String buildVerifyUrl(final String rawToken) {
        return UriComponentsBuilder.fromUriString(appProperties.getApiBaseUrl())
                .path(VERIFY_PATH)
                .queryParam("token", rawToken)
                .toUriString();
    }

    private static String generateRawToken() {
        final var tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

}
