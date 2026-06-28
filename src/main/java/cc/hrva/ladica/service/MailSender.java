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
    private static final String MAGIC_LINK_SUBJECT = "Your Ladica sign-in link";
    private static final String MAGIC_LINK_URL_VARIABLE = "magic_link_url";

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    public void sendMagicLink(final String recipient, final String magicLinkUrl) {
        final var context = new Context();
        context.setVariable(MAGIC_LINK_URL_VARIABLE, magicLinkUrl);
        final var htmlBody = templateEngine.process(MAGIC_LINK_TEMPLATE, context);

        sendHtmlEmail(recipient, MAGIC_LINK_SUBJECT, htmlBody);
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
