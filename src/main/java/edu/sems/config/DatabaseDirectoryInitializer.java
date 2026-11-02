package edu.sems.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates the parent directory for a file-based SQLite database before the pool opens it.
 * SQLite creates the database file, but it does not create missing parent directories.
 * Paths are resolved with {@link Path} so the same code works on Windows and macOS.
 */
@Component
public class DatabaseDirectoryInitializer implements BeanPostProcessor {

    static final String JDBC_SQLITE_PREFIX = "jdbc:sqlite:";

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof HikariDataSource dataSource) {
            ensureParentDirectoryExists(dataSource.getJdbcUrl());
        }
        return bean;
    }

    static void ensureParentDirectoryExists(String jdbcUrl) {
        Path databaseFile = databaseFile(jdbcUrl);
        if (databaseFile == null) {
            return;
        }
        Path parent = databaseFile.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the database directory " + parent, exception);
        }
    }

    static Path databaseFile(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(JDBC_SQLITE_PREFIX)) {
            return null;
        }
        String location = jdbcUrl.substring(JDBC_SQLITE_PREFIX.length());
        int queryStart = location.indexOf('?');
        if (queryStart >= 0) {
            location = location.substring(0, queryStart);
        }
        if (location.startsWith("file:")) {
            location = location.substring("file:".length());
        }
        if (location.isBlank() || ":memory:".equals(location)) {
            return null;
        }
        return Path.of(location);
    }
}
