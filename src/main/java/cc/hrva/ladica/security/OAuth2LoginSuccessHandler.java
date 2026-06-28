package cc.hrva.ladica.security;

import cc.hrva.ladica.configuration.properties.AppProperties;
import cc.hrva.ladica.model.UserAccount;
import cc.hrva.ladica.model.enums.AuthProvider;
import cc.hrva.ladica.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String EMAIL_ATTRIBUTE = "email";

    private final UserAccountRepository userAccountRepository;
    private final TokenProvider tokenProvider;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication) throws IOException {

        final var oAuth2User = (OAuth2User) authentication.getPrincipal();
        final var email = extractEmail(oAuth2User);

        if (StringUtils.isBlank(email)) {
            log.warn("OAuth2 login failed: no email returned from provider");
            response.sendRedirect(buildErrorRedirect("no_email"));
            return;
        }

        final var userAccount = findOrCreateUser(email);

        if (!userAccount.isActive()) {
            log.warn("OAuth2 login blocked: inactive account email={}", email);
            response.sendRedirect(buildErrorRedirect("account_inactive"));
            return;
        }

        userAccount.setLastLoginAt(LocalDateTime.now());
        userAccountRepository.save(userAccount);

        final var jwt = tokenProvider.createToken(email, List.of(new SimpleGrantedAuthority(ROLE_USER)));

        log.debug("OAuth2 login success email={}", email);
        response.sendRedirect(buildTokenRedirect(jwt));
    }

    private UserAccount findOrCreateUser(final String email) {
        return userAccountRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email));
    }

    private UserAccount createGoogleUser(final String email) {
        final var newUser = UserAccount.builder()
                .email(email)
                .authProvider(AuthProvider.GOOGLE)
                .active(true)
                .build();

        final var savedUser = userAccountRepository.save(newUser);
        log.debug("Created OAuth2 user email={}", email);

        return savedUser;
    }

    private String extractEmail(final OAuth2User oAuth2User) {
        final var email = oAuth2User.getAttribute(EMAIL_ATTRIBUTE);

        return email instanceof String emailValue ? emailValue : null;
    }

    private String buildTokenRedirect(final String jwt) {
        return "%s/auth/callback?token=%s".formatted(
                appProperties.getFrontendUrl(),
                URLEncoder.encode(jwt, StandardCharsets.UTF_8));
    }

    private String buildErrorRedirect(final String error) {
        return "%s/auth/callback?error=%s".formatted(appProperties.getFrontendUrl(), error);
    }

}
