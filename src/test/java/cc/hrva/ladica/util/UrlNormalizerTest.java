package cc.hrva.ladica.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlNormalizerTest {

    @Test
    void stripsSchemeWwwAndTrailingSlash() {
        assertThat(UrlNormalizer.normalize("https://www.Konzum.hr/")).isEqualTo("konzum.hr");
        assertThat(UrlNormalizer.normalize("http://x.com/a")).isEqualTo("x.com/a");
    }

    @Test
    void prependsHttpsAndKeepsPath() {
        assertThat(UrlNormalizer.normalize("konzum.hr")).isEqualTo("konzum.hr");
        assertThat(UrlNormalizer.normalize("https://www.coolinarika.com/recepti")).isEqualTo("coolinarika.com/recepti");
    }

    @Test
    void keepsQuerySoDistinctPagesStayDistinct() {
        assertThat(UrlNormalizer.normalize("site.com/a?id=1")).isEqualTo("site.com/a?id=1");
        assertThat(UrlNormalizer.normalize("site.com/a?id=2")).isEqualTo("site.com/a?id=2");
        assertThat(UrlNormalizer.normalize("site.com/a?id=1"))
                .isNotEqualTo(UrlNormalizer.normalize("site.com/a?id=2"));
    }

    @Test
    void stripsTrailingSlashFromPathButKeepsQuery() {
        assertThat(UrlNormalizer.normalize("https://x.com/a/")).isEqualTo("x.com/a");
        assertThat(UrlNormalizer.normalize("https://x.com/a/?id=1")).isEqualTo("x.com/a?id=1");
    }

    @Test
    void returnsEmptyForBlankInput() {
        assertThat(UrlNormalizer.normalize("")).isEmpty();
        assertThat(UrlNormalizer.normalize(null)).isEmpty();
    }

}
