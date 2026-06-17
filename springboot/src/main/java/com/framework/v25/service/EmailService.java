package com.framework.v25.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String         from;

    /**
     * FIXED: @Value injected via constructor parameter — not field injection.
     * Field-level @Value with @RequiredArgsConstructor causes null at runtime.
     */
    @Autowired
    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.git.invitation-from}") String from) {
        this.mailSender = mailSender;
        this.from       = from;
    }

    @Async
    public void sendGitInvitation(String toEmail, String projectName, String inviterName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Git invitation — " + projectName);
            message.setText(String.format(
                    "Hi,%n%n" +
                    "%s has invited you to collaborate on \"%s\" in Framework v2.5.%n%n" +
                    "Please check your Git provider for the repository invitation.%n%n" +
                    "— Framework v2.5",
                    inviterName, projectName
            ));
            mailSender.send(message);
            log.info("Git invitation sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}: {}", toEmail, e.getMessage());
        }
    }
}