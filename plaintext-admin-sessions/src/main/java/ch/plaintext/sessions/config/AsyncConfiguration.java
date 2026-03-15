package ch.plaintext.sessions.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enable async processing for session tracking.
 * This allows session updates to happen in the background without blocking the main request.
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
}
