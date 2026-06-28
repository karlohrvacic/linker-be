package cc.hrva.ladica.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("app")
public class AppProperties {

    @NotBlank
    private String apiBaseUrl;

    @NotBlank
    private String frontendUrl;

    @NotBlank
    private String jwtBase64Secret;

    @Positive
    private long jwtTokenValiditySeconds;

    @Positive
    private long magicLinkMaxRequestsPerHour;

    @Positive
    private long magicLinkMaxVerifyAttemptsPerHour;

    @Positive
    private long magicLinkTtlSeconds;

    @NotBlank
    private String mailFrom;

    @NotBlank
    private String mailReplyTo;

    @Positive
    private long maxLinksPerUser;

}
