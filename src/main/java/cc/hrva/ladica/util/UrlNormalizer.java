package cc.hrva.ladica.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UrlNormalizer {

    private static final Pattern SCHEME_PATTERN = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_SLASHES_PATTERN = Pattern.compile("^/+");
    private static final Pattern LEADING_WWW_PATTERN = Pattern.compile("^www\\.");
    private static final String ROOT_PATH = "/";

    public static String normalize(final String rawUrl) {
        if (StringUtils.isBlank(rawUrl)) {
            return "";
        }

        final var trimmedUrl = rawUrl.trim();

        return toPrettyHost(ensureScheme(trimmedUrl), trimmedUrl);
    }

    private static String toPrettyHost(final String absoluteUrl, final String fallback) {
        try {
            final var uri = URI.create(absoluteUrl);
            final var host = uri.getHost();
            if (StringUtils.isBlank(host)) {
                return fallback;
            }

            final var path = stripTrailingSlash(StringUtils.defaultString(uri.getRawPath()));
            final var query = uri.getRawQuery();
            final var pathWithQuery = StringUtils.isBlank(query) ? path : "%s?%s".formatted(path, query);
            final var hostWithPath = "%s%s".formatted(host.toLowerCase(Locale.ROOT), pathWithQuery);

            return LEADING_WWW_PATTERN.matcher(hostWithPath).replaceFirst("");
        } catch (final IllegalArgumentException e) {
            return fallback;
        }
    }

    private static String stripTrailingSlash(final String path) {
        if (ROOT_PATH.equals(path) || path.isEmpty()) {
            return "";
        }

        return path.endsWith(ROOT_PATH) ? path.substring(0, path.length() - 1) : path;
    }

    private static String ensureScheme(final String trimmedUrl) {
        if (SCHEME_PATTERN.matcher(trimmedUrl).find()) {
            return trimmedUrl;
        }

        final var withoutLeadingSlashes = LEADING_SLASHES_PATTERN.matcher(trimmedUrl).replaceFirst("");

        return "https://%s".formatted(withoutLeadingSlashes);
    }

}
