package cc.hrva.ladica.security;

import cc.hrva.ladica.configuration.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MagicLinkRateLimiter {

    private static final long WINDOW_MILLIS = Duration.ofHours(1).toMillis();
    private static final String REQUEST_EMAIL_PREFIX = "request:email:";
    private static final String REQUEST_IP_PREFIX = "request:ip:";
    private static final String VERIFY_IP_PREFIX = "verify:ip:";

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final AppProperties appProperties;

    public boolean tryRequest(final String email, final String clientIp) {
        final var now = System.currentTimeMillis();
        final var maxRequests = appProperties.getMagicLinkMaxRequestsPerHour();

        if (isAtLimit(REQUEST_EMAIL_PREFIX + email, maxRequests, now)
                || isAtLimit(REQUEST_IP_PREFIX + clientIp, maxRequests, now)) {
            return false;
        }

        consume(REQUEST_EMAIL_PREFIX + email, now);
        consume(REQUEST_IP_PREFIX + clientIp, now);

        return true;
    }

    public boolean tryVerify(final String clientIp) {
        final var now = System.currentTimeMillis();

        if (isAtLimit(VERIFY_IP_PREFIX + clientIp, appProperties.getMagicLinkMaxVerifyAttemptsPerHour(), now)) {
            return false;
        }

        consume(VERIFY_IP_PREFIX + clientIp, now);

        return true;
    }

    private boolean isAtLimit(final String key, final long maxPerWindow, final long now) {
        final var window = windows.get(key);
        if (window == null || isExpired(window, now)) {
            return false;
        }

        return window.count() >= maxPerWindow;
    }

    private void consume(final String key, final long now) {
        windows.compute(key, (existingKey, existingWindow) -> {
            if (existingWindow == null || isExpired(existingWindow, now)) {
                return new Window(now, 1);
            }

            return new Window(existingWindow.startMillis(), existingWindow.count() + 1);
        });
    }

    private static boolean isExpired(final Window window, final long now) {
        return now - window.startMillis() >= WINDOW_MILLIS;
    }

    private record Window(long startMillis, int count) {
    }

}
