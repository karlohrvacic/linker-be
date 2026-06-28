package cc.hrva.ladica.security;

import cc.hrva.ladica.configuration.properties.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MagicLinkRateLimiterTest {

    private static final String EMAIL = "user@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String IP = "10.0.0.1";
    private static final String OTHER_IP = "10.0.0.2";

    @Mock
    private AppProperties appProperties;

    private MagicLinkRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new MagicLinkRateLimiter(appProperties);
    }

    @Test
    void allowsRequestsUpToTheCapThenBlocks() {
        when(appProperties.getMagicLinkMaxRequestsPerHour()).thenReturn(3L);

        assertThat(rateLimiter.tryRequest(EMAIL, IP)).isTrue();
        assertThat(rateLimiter.tryRequest(EMAIL, IP)).isTrue();
        assertThat(rateLimiter.tryRequest(EMAIL, IP)).isTrue();
        assertThat(rateLimiter.tryRequest(EMAIL, IP)).isFalse();
    }

    @Test
    void blocksWhenEmailCapReachedEvenFromADifferentIp() {
        when(appProperties.getMagicLinkMaxRequestsPerHour()).thenReturn(2L);

        assertThat(rateLimiter.tryRequest(EMAIL, IP)).isTrue();
        assertThat(rateLimiter.tryRequest(EMAIL, IP)).isTrue();

        assertThat(rateLimiter.tryRequest(EMAIL, OTHER_IP)).isFalse();
    }

    @Test
    void blocksWhenIpCapReachedEvenForADifferentEmail() {
        when(appProperties.getMagicLinkMaxRequestsPerHour()).thenReturn(2L);

        assertThat(rateLimiter.tryRequest(EMAIL, IP)).isTrue();
        assertThat(rateLimiter.tryRequest(OTHER_EMAIL, IP)).isTrue();

        assertThat(rateLimiter.tryRequest("third@example.com", IP)).isFalse();
    }

    @Test
    void tracksDistinctEmailAndIpCombinationsIndependently() {
        when(appProperties.getMagicLinkMaxRequestsPerHour()).thenReturn(1L);

        assertThat(rateLimiter.tryRequest(EMAIL, IP)).isTrue();
        assertThat(rateLimiter.tryRequest(EMAIL, IP)).isFalse();

        assertThat(rateLimiter.tryRequest(OTHER_EMAIL, OTHER_IP)).isTrue();
    }

    @Test
    void allowsVerifyAttemptsUpToTheCapThenBlocksPerIp() {
        when(appProperties.getMagicLinkMaxVerifyAttemptsPerHour()).thenReturn(2L);

        assertThat(rateLimiter.tryVerify(IP)).isTrue();
        assertThat(rateLimiter.tryVerify(IP)).isTrue();
        assertThat(rateLimiter.tryVerify(IP)).isFalse();

        assertThat(rateLimiter.tryVerify(OTHER_IP)).isTrue();
    }

}
