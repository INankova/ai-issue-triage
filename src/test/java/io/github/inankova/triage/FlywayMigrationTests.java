package io.github.inankova.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Flyway must apply our migrations when the application starts, against the same
 * PostgreSQL 18 + pgvector image we use locally, with no local database needed (ROADMAP P0.14).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlywayMigrationTests {

	private final Flyway flyway;

	private final JdbcClient jdbcClient;

	FlywayMigrationTests(@Autowired Flyway flyway, @Autowired JdbcClient jdbcClient) {
		this.flyway = flyway;
		this.jdbcClient = jdbcClient;
	}

	@Test
	void initMigrationIsAppliedOnStartup() {
		assertThat(flyway.info().applied())
			.extracting(migration -> migration.getVersion().getVersion(), MigrationInfo::getState)
			.contains(tuple("1", MigrationState.SUCCESS));
	}

	@Test
	void vectorExtensionIsInstalled() {
		List<String> extensions = jdbcClient.sql("SELECT extname FROM pg_extension").query(String.class).list();

		assertThat(extensions).contains("vector");
	}

}
