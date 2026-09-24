package dev.course.persistence.support;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class EmbeddedDatabase {

    private EmbeddedDatabase() {
    }

    public static void initialize(String databaseName) {
        try {
            Class.forName("org.h2.Driver");
            try (Connection connection = DriverManager.getConnection(url(databaseName), "sa", "")) {
                ScriptRunner runner = new ScriptRunner(connection);
                runner.setStopOnError(true);
                runner.setLogWriter(null);
                runner.setErrorLogWriter(null);
                run(runner, "schema-h2.sql");
                run(runner, "data-h2.sql");
            }
        } catch (ClassNotFoundException | SQLException | IOException exception) {
            throw new IllegalStateException("无法初始化内存测试数据库", exception);
        }
    }

    public static Properties properties(String databaseName) {
        Properties properties = new Properties();
        properties.setProperty("jdbc.driver", "org.h2.Driver");
        properties.setProperty("jdbc.url", url(databaseName));
        properties.setProperty("jdbc.username", "sa");
        properties.setProperty("jdbc.password", "");
        return properties;
    }

    private static void run(ScriptRunner runner, String resource) throws IOException {
        try (InputStream stream = Resources.getResourceAsStream(resource);
             Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            runner.runScript(reader);
        }
    }

    private static String url(String databaseName) {
        return "jdbc:h2:mem:" + databaseName
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    }
}
