package io.github.inankova.triage.shared;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The app's single source of "now". Classes ask for a {@link Clock} instead of calling {@code Instant.now()}, so a
 * test can hand them a fixed clock (like {@code TimeProvider} in .NET).
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
