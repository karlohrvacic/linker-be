package cc.hrva.ladica.service;

import cc.hrva.ladica.configuration.properties.AppProperties;
import cc.hrva.ladica.exception.InvalidTokenException;
import cc.hrva.ladica.model.LoginToken;
import cc.hrva.ladica.model.UserAccount;
import cc.hrva.ladica.model.enums.AuthProvider;
import cc.hrva.ladica.repository.LoginTokenRepository;
import cc.hrva.ladica.repository.UserAccountRepository;
import cc.hrva.ladica.security.TokenProvider;
import cc.hrva.ladica.service.impl.DefaultMagicLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultMagicLinkServiceTest {

    @Mock
    private LoginTokenRepository loginTokenRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private MailSender mailSender;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private DefaultMagicLinkService service;

    @Test
    void requestLinkBuildsVerifyUrlFromConfiguredApiBaseUrl() {
        when(appProperties.getMagicLinkTtlSeconds()).thenReturn(900L);
        when(appProperties.getApiBaseUrl()).thenReturn("http://localhost:8080");

        service.requestLink("user@example.com", "hr");

        verify(mailSender).sendMagicLink(
                eq("user@example.com"),
                argThat(url -> url.startsWith("http://localhost:8080/api/v1/auth/magic-link/verify?token=")),
                eq("hr"));
    }

    @Test
    void verifyConsumesTokenAndReturnsJwtForNewUser() {
        final var loginToken = LoginToken.builder()
                .email("new@b.com")
                .tokenHash(DefaultMagicLinkService.hash("raw123"))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        when(loginTokenRepository.findByTokenHash(DefaultMagicLinkService.hash("raw123")))
                .thenReturn(Optional.of(loginToken));
        when(userAccountRepository.findByEmail("new@b.com")).thenReturn(Optional.empty());
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.createToken(eq("new@b.com"), any())).thenReturn("JWT");

        final var jwt = service.verify("raw123");

        assertThat(jwt).isEqualTo("JWT");
        assertThat(loginToken.getUsedAt()).isNotNull();
        verify(userAccountRepository).save(argThat(savedUser -> savedUser.getAuthProvider() == AuthProvider.EMAIL));
    }

    @Test
    void verifyRejectsExpiredToken() {
        final var loginToken = LoginToken.builder()
                .email("e@b.com")
                .tokenHash(DefaultMagicLinkService.hash("r"))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(loginTokenRepository.findByTokenHash(DefaultMagicLinkService.hash("r")))
                .thenReturn(Optional.of(loginToken));

        assertThatThrownBy(() -> service.verify("r")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyRejectsUsedToken() {
        final var loginToken = LoginToken.builder()
                .email("e@b.com")
                .tokenHash(DefaultMagicLinkService.hash("r2"))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .usedAt(LocalDateTime.now())
                .build();
        when(loginTokenRepository.findByTokenHash(DefaultMagicLinkService.hash("r2")))
                .thenReturn(Optional.of(loginToken));

        assertThatThrownBy(() -> service.verify("r2")).isInstanceOf(InvalidTokenException.class);
    }

}
