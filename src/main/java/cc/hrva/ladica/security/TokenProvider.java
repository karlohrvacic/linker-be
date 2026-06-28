package cc.hrva.ladica.security;

import cc.hrva.ladica.configuration.properties.AppProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final long SECONDS_TO_MILLISECONDS = 1000L;

    private final AppProperties appProperties;
    private final SecretKey key;

    public TokenProvider(final AppProperties appProperties) {
        this.appProperties = appProperties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(appProperties.getJwtBase64Secret()));
    }

    public String createToken(final String email, final Collection<? extends GrantedAuthority> authorities) {
        return createToken(email, authorities, appProperties.getJwtTokenValiditySeconds());
    }

    public String createToken(final String email, final Collection<? extends GrantedAuthority> authorities, final long ttlSeconds) {
        final var now = new Date().getTime();
        final var validity = new Date(now + SECONDS_TO_MILLISECONDS * ttlSeconds);
        final var joinedAuthorities = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(email)
                .claim(AUTHORITIES_KEY, joinedAuthorities)
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public Authentication getAuthentication(final String token) {
        final var claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        final var authorities = Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();

        final var principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(final String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authToken);

            return true;
        } catch (final JwtException | IllegalArgumentException e) {
            log.info("Invalid JWT token.");
            log.trace("Invalid JWT token trace.", e);
        }

        return false;
    }

}
