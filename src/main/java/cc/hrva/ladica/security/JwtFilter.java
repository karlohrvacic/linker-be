package cc.hrva.ladica.security;

import cc.hrva.ladica.model.UserAccount;
import cc.hrva.ladica.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends GenericFilterBean {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final UserAccountRepository userAccountRepository;

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain)
            throws IOException, ServletException {

        final var httpServletRequest = (HttpServletRequest) servletRequest;
        final var jwt = resolveToken(httpServletRequest);

        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            final var authentication = tokenProvider.getAuthentication(jwt);
            if (isActiveUser(authentication.getName())) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean isActiveUser(final String email) {
        return userAccountRepository.findByEmail(email)
                .map(UserAccount::isActive)
                .orElse(false);
    }

    private String resolveToken(final HttpServletRequest request) {
        final var bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

}
