package cc.hrva.ladica.security;

import cc.hrva.ladica.configuration.properties.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenProviderTest {

    @Test
    void createdTokenValidatesAndCarriesEmailAndAuthorities() {
        final var appProperties = new AppProperties();
        appProperties.setJwtBase64Secret(Base64.getEncoder().encodeToString("x".repeat(64).getBytes()));
        appProperties.setJwtTokenValiditySeconds(3600);
        final var tokenProvider = new TokenProvider(appProperties);

        final var token = tokenProvider.createToken("a@b.com", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(tokenProvider.validateToken(token)).isTrue();
        final var authentication = tokenProvider.getAuthentication(token);
        assertThat(authentication.getName()).isEqualTo("a@b.com");
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER");
    }

    @Test
    void invalidTokenFailsValidation() {
        final var appProperties = new AppProperties();
        appProperties.setJwtBase64Secret(Base64.getEncoder().encodeToString("y".repeat(64).getBytes()));
        appProperties.setJwtTokenValiditySeconds(3600);

        assertThat(new TokenProvider(appProperties).validateToken("not.a.jwt")).isFalse();
    }

}
