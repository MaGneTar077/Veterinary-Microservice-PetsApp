package com.MyAnimaLog.Veterinary;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB_POOL_MAX_SIZE / DB_CONNECTION_TIMEOUT_MS are the only supported way to tune the Hikari pool
 * from the environment (see application.yaml's spring.datasource.hikari block). Setting
 * SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE / SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT as real
 * OS environment variables used to crash the app on startup instead — see
 * {@code HikariPoolConfigurationRegressionTest} for that reproduction, kept as its own process-level
 * test. Fixed in "pool de Hikari configurable sin romper el arranque".
 */
@SpringBootTest(properties = {"DB_POOL_MAX_SIZE=5", "DB_CONNECTION_TIMEOUT_MS=12345"})
@ActiveProfiles("test")
class HikariPoolConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void hikariDataSource_shouldUseConfiguredPoolSizeAndTimeout() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        HikariDataSource hikari = (HikariDataSource) dataSource;

        assertThat(hikari.getMaximumPoolSize()).isEqualTo(5);
        assertThat(hikari.getConnectionTimeout()).isEqualTo(12345L);
        // connection-init-sql must still apply too -- proves the fix didn't just move the crash elsewhere.
        assertThat(hikari.getConnectionInitSql())
                .isEqualTo("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL READ COMMITTED");
    }
}
