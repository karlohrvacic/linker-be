package cc.hrva.ladica.security;

import cc.hrva.ladica.configuration.properties.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Sends OAuth2 login failures back to the frontend callback (?error=login_failed)
 * instead of Spring's default /login page, which does not exist on this API-only
 * backend (a 404 there is what surfaced as NoResourceFoundException). The frontend
 * /auth/callback already renders an error state for ?error=.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final AppProperties appProperties;

    @Override
    public void onAuthenticationFailure(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException exception) throws IOException {

        log.warn("OAuth2 login failed: {}", exception.getMessage());
        response.sendRedirect("%s/auth/callback?error=login_failed".formatted(appProperties.getFrontendUrl()));
    }

}
