package edu.sems;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SemsApplicationTests {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        Path database = tempDir.resolve("sems-test.db");
        String path = database.toAbsolutePath().toString().replace('\\', '/');
        registry.add("SEMS_DB_PATH", () -> path);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Test
    void applicationContext_loadsAgainstTemporarySqlite() throws Exception {
        assertThat(environment.getProperty("spring.datasource.url")).contains("sems-test.db");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet foreignKeys = statement.executeQuery("PRAGMA foreign_keys")) {
                assertThat(foreignKeys.next()).isTrue();
                assertThat(foreignKeys.getInt(1)).isEqualTo(1);
            }
            try (ResultSet tables = statement.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
                List<String> names = new ArrayList<>();
                while (tables.next()) {
                    names.add(tables.getString("name"));
                }
                assertThat(names).contains("audit_events", "employees", "leave_requests", "users");
            }
            assertThatThrownBy(() -> statement.executeUpdate(
                    "INSERT INTO leave_requests (employee_id, start_date, end_date, reason, status, created_at) "
                            + "VALUES (999999, '2026-11-10', '2026-11-11', 'Bogus employee', 'PENDING', '2026-11-03T00:00:00Z')"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
