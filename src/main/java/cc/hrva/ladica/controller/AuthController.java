package cc.hrva.ladica.controller;

import cc.hrva.ladica.configuration.properties.AppProperties;
import cc.hrva.ladica.dto.MagicLinkRequest;
import cc.hrva.ladica.exception.InvalidTokenException;
import cc.hrva.ladica.security.ClientIpResolver;
import cc.hrva.ladica.security.MagicLinkRateLimiter;
import cc.hrva.ladica.service.MagicLinkService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
public class AuthController {

    private static final String ERROR_INVALID = "invalid";
    private static final String ERROR_RATE_LIMITED = "rate_limited";

    private final MagicLinkService magicLinkService;
    private final MagicLinkRateLimiter magicLinkRateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final AppProperties appProperties;

    @Operation(summary = "Request a magic link", description = "Send a single-use sign-in link to the given email. Always returns 200 to avoid revealing whether the email is registered.")
    @PostMapping("/magic-link")
    public ResponseEntity<Void> requestMagicLink(@Valid @RequestBody final MagicLinkRequest request, final HttpServletRequest httpRequest) {
        log.info("Magic link requested");

        if (magicLinkRateLimiter.tryRequest(request.email(), clientIpResolver.resolve(httpRequest))) {
            magicLinkService.requestLink(request.email());
        } else {
            log.info("Magic link request rate limited");
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Verify a magic link", description = "Consume the single-use token and redirect to the frontend callback with a JWT, or with an error when the token is invalid.")
    @GetMapping("/magic-link/verify")
    public ResponseEntity<Void> verifyMagicLink(@RequestParam final String token, final HttpServletRequest httpRequest) {
        final var redirectLocation = resolveVerifyRedirect(token, clientIpResolver.resolve(httpRequest));

        return ResponseEntity.status(HttpStatus.FOUND).location(redirectLocation).build();
    }

    private URI resolveVerifyRedirect(final String token, final String clientIp) {
        if (!magicLinkRateLimiter.tryVerify(clientIp)) {
            log.info("Magic link verification rate limited");

            return URI.create(buildCallbackUrl("error", ERROR_RATE_LIMITED));
        }

        try {
            final var jwt = magicLinkService.verify(token);

            return URI.create(buildCallbackUrl("token", jwt));
        } catch (final InvalidTokenException e) {
            log.info("Magic link verification failed: {}", e.getMessage());

            return URI.create(buildCallbackUrl("error", ERROR_INVALID));
        }
    }

    private String buildCallbackUrl(final String parameter, final String value) {
        return "%s/auth/callback?%s=%s".formatted(
                appProperties.getFrontendUrl(),
                parameter,
                URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

}
