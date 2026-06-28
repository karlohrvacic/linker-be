package cc.hrva.ladica.service;

import cc.hrva.ladica.configuration.properties.AppProperties;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component("magicLinkMailSender")
@RequiredArgsConstructor
public class MailSender {

    private static final String MAGIC_LINK_TEMPLATE = "magic-link";
    private static final String MAGIC_LINK_URL_VARIABLE = "magic_link_url";
    private static final String LOGO_PATH = "/assets/icon-192.png";

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    /** Croatian is the default; only an explicit "en" switches to English (mirrors the app). */
    public void sendMagicLink(final String recipient, final String magicLinkUrl, final String lang) {
        final var english = "en".equalsIgnoreCase(lang);

        final var context = new Context();
        context.setVariable(MAGIC_LINK_URL_VARIABLE, magicLinkUrl);
        context.setVariable("logoUrl", appProperties.getFrontendUrl() + LOGO_PATH);
        context.setVariable("lang", english ? "en" : "hr");
        context.setVariable("title", english ? "Sign in to Ladica" : "Prijava u Ladicu");
        context.setVariable("intro", english
                ? "Tap the button below to sign in. The link works once and expires shortly."
                : "Dodirni gumb ispod za prijavu. Poveznica vrijedi jednokratno i uskoro istječe.");
        context.setVariable("buttonLabel", english ? "Sign in" : "Prijavi se");
        context.setVariable("fallback", english
                ? "If the button doesn't work, copy this link into your browser:"
                : "Ako gumb ne radi, kopiraj ovu poveznicu u preglednik:");
        context.setVariable("ignore", english
                ? "If you didn't request this email, you can safely ignore it."
                : "Ako nisi zatražio/la ovu poruku, slobodno je zanemari.");
        context.setVariable("footer", english
                ? "Ladica · save links, open with one tap"
                : "Ladica · spremi linkove, otvori jednim dodirom");

        final var subject = english ? "Your Ladica sign-in link" : "Tvoja poveznica za prijavu u Ladicu";
        final var htmlBody = templateEngine.process(MAGIC_LINK_TEMPLATE, context);

        sendHtmlEmail(recipient, subject, htmlBody);
    }

    private void sendHtmlEmail(final String recipient, final String subject, final String htmlBody) {
        try {
            final var mimeMessage = javaMailSender.createMimeMessage();
            final var messageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            messageHelper.setFrom(appProperties.getMailFrom());
            messageHelper.setReplyTo(appProperties.getMailReplyTo());
            messageHelper.setTo(recipient);
            messageHelper.setSubject(subject);
            messageHelper.setText(htmlBody, true);

            javaMailSender.send(mimeMessage);
            log.info("Sent {} email to {}", subject, recipient);
        } catch (final MessagingException e) {
            log.error("Failed to send email to {}", recipient, e);
            throw new IllegalStateException("Failed to send email", e);
        }
    }

}
