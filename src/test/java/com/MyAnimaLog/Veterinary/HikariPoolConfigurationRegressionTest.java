package com.MyAnimaLog.Veterinary;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the historical bug directly: with SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT set as a
 * real OS environment variable (not a .env-imported property, not a JVM system property passed via
 * -D — neither of those reproduces it), Spring's Binder ends up calling
 * {@code HikariDataSource.getConnection()} while resolving the property name (it collides with the
 * no-arg {@code getConnection()} getter the DataSource interface exposes), sealing the pool before
 * connection-init-sql can be applied. Confirmed with a live debugger: the stack trace runs
 * {@code JavaBeanBinder$BeanProperty.getValue -> HikariDataSource.getConnection -> HikariConfig.seal}.
 *
 * <p>This can only be demonstrated in a separate OS process: the current JVM's real environment
 * variables can't be mutated, and {@code @SpringBootTest(properties=...)}/system properties use a
 * different, lower-precedence PropertySource that does not reproduce this specific failure.
 *
 * <p>This test is independent of this repo's own configuration — it documents a Spring
 * Boot/HikariCP interaction we must never reintroduce by setting SPRING_DATASOURCE_HIKARI_* directly
 * in any environment (see DB_POOL_MAX_SIZE/DB_CONNECTION_TIMEOUT_MS in application.yaml instead).
 */
class HikariPoolConfigurationRegressionTest {

    @Test
    void applicationProcess_shouldFailToStart_whenHikariPropertiesComeFromRawSpringDatasourceHikariEnvVars() throws Exception {
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder processBuilder = new ProcessBuilder(
                javaBin, "-cp", classpath, "com.MyAnimaLog.Veterinary.VeterinaryApplication");
        processBuilder.environment().put("SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT", "8000");
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();

        // Drain stdout WHILE the process runs, on its own thread -- otherwise Spring Boot's startup
        // logging can fill the OS pipe buffer before we ever call waitFor(), deadlocking the child
        // process on a blocked write and making this test wait out the full timeout for no reason.
        Thread drainer = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (output) {
                        output.append(line).append('\n');
                    }
                }
            } catch (Exception ignored) {
                // process stream closed from the other end, nothing more to drain
            }
        });
        drainer.setDaemon(true);
        drainer.start();

        boolean exited = process.waitFor(30, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
        }
        drainer.join(5_000);

        String fullOutput;
        synchronized (output) {
            fullOutput = output.toString();
        }
        assertThat(fullOutput)
                .as("expected the historical Hikari binding crash; full output:\n%s", fullOutput)
                .contains("The configuration of the pool is sealed once started")
                .contains("spring.datasource.hikari");
        assertThat(fullOutput).doesNotContain("Started VeterinaryApplication");
    }
}
