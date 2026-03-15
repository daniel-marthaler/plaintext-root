package ch.plaintext.email.listener;

import ch.plaintext.PlaintextEmailReceiver;
import ch.plaintext.PlaintextIncomingEmailListener;
import ch.plaintext.email.model.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Test-Listener für eingehende E-Mails.
 * Gibt empfangene E-Mails im Log aus.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailTestListener implements PlaintextIncomingEmailListener {

    private final PlaintextEmailReceiver emailReceiver;

    @Override
    public void onEmailReceived(Long emailId, String mandat, String configName) {
        log.info("=== EMAIL TEST LISTENER ===");
        log.info("New email received!");
        log.info("Email ID: {}", emailId);
        log.info("Mandate: {}", mandat);
        log.info("Config: {}", configName);

        // Subject sofort ausgeben, bevor Email Details geladen werden
        emailReceiver.readEmail(emailId).ifPresent(emailObj -> {
            if (emailObj instanceof Email email) {
                log.info("SUBJECT -> {}", email.getSubject());
            }
        });

        // Email Details abrufen und loggen
        emailReceiver.readEmail(emailId).ifPresent(emailObj -> {
            if (emailObj instanceof Email email) {
                log.info("=== EMAIL DETAILS ===");
                log.info("From: {}", email.getFromAddress());
                log.info("To: {}", email.getToAddress());
                log.info("Direction: {}", email.getDirection());
                log.info("Status: {}", email.getStatus());
                log.info("Created At: {}", email.getCreatedAt());

                // Body nur die ersten 200 Zeichen loggen
                String body = email.getBody();
                if (body != null) {
                    String bodyPreview = body.length() > 200
                        ? body.substring(0, 200) + "..."
                        : body;
                    log.info("Body Preview: {}", bodyPreview);
                }

                log.info("HTML: {}", email.isHtml());
                log.info("======================");
            }
        });
    }

    @Override
    public String getListenerName() {
        return "Email Test Listener";
    }

    @Override
    public List<String> getConfigNamesToListenTo() {
        // Nur auf die "maintenance" Konfiguration hören
        return List.of("maintenance");
    }
}
