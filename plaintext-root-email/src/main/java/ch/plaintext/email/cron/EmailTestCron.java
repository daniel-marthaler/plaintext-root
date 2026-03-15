package ch.plaintext.email.cron;

import ch.plaintext.PlaintextCron;
import ch.plaintext.PlaintextEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Test-Cron zum Versenden einer Test-Email über die Konfiguration "maintenance"
 * (Mandant: "plaintext") an daniel@marthaler.io
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailTestCron implements PlaintextCron {

    private final PlaintextEmailSender emailSender;

    @Override
    public boolean isGlobal() {
        return true; // Global = mandanten-unabhängig
    }

    @Override
    public String getDisplayName() {
        return "Email Test Versand";
    }

    @Override
    public String getDefaultCronExpression() {
        // Deaktiviert per Default (null = nicht geplant)
        // Kann manuell in der Cron-Verwaltung aktiviert werden
        return null;
    }

    @Override
    public void run(String mandant) {
        // Ignoriere den übergebenen Mandanten, da dieser Cron global ist
        log.info("Email test cron started (global)");

        try {
            // Zeitstempel für die Test-Email
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));

            // Test-Email über PlaintextEmailSender senden mit "maintenance" Konfiguration
            Long emailId = emailSender.sendEmail(
                    "maintenance", // config name
                    "daniel@marthaler.io", // to
                    "Test-Email von Plaintext - " + timestamp, // subject
                    buildTestEmailBody(timestamp), // body
                    true // html
            );

            log.info("Test email queued successfully with ID: {}", emailId);
            log.info("Email test cron completed");

        } catch (Exception e) {
            log.error("Email test cron failed", e);
        }
    }

    /**
     * Erstellt den HTML-Body für die Test-Email
     */
    private String buildTestEmailBody(String timestamp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                            border: 1px solid #ddd;
                            border-radius: 5px;
                        }
                        .header {
                            background-color: #4CAF50;
                            color: white;
                            padding: 15px;
                            text-align: center;
                            border-radius: 5px 5px 0 0;
                        }
                        .content {
                            padding: 20px;
                            background-color: #f9f9f9;
                        }
                        .info-box {
                            background-color: #e7f3ff;
                            border-left: 4px solid #2196F3;
                            padding: 10px;
                            margin: 15px 0;
                        }
                        .footer {
                            text-align: center;
                            padding: 15px;
                            font-size: 12px;
                            color: #777;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Plaintext Email Test</h1>
                        </div>
                        <div class="content">
                            <p>Hallo,</p>
                            <p>Dies ist eine automatisch generierte Test-Email vom Plaintext Email-System.</p>

                            <div class="info-box">
                                <strong>Email-Details:</strong><br>
                                <strong>Konfiguration:</strong> maintenance<br>
                                <strong>Zeitstempel:</strong> %s
                            </div>

                            <p>Wenn Sie diese Email erhalten, funktioniert der Email-Versand korrekt.</p>

                            <p>Mit freundlichen Grüssen,<br>
                            Ihr Plaintext Team</p>
                        </div>
                        <div class="footer">
                            Plaintext Email Test System - Automatisch generiert
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(timestamp);
    }
}
