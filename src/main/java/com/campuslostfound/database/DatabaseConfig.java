package com.campuslostfound.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/campus_lost_found?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static String url;
    private static String user;
    private static String password;
    private static boolean initialized = false;

    private DatabaseConfig() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        Properties props = new Properties();
        boolean loadedFromFile = false;

        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
                loadedFromFile = true;
            }
        } catch (Exception ignored) {
        }

        if (!loadedFromFile) {
            File localFile = new File("src/main/resources/db.properties");
            if (localFile.exists()) {
                try (InputStream in = new FileInputStream(localFile)) {
                    props.load(in);
                    loadedFromFile = true;
                } catch (Exception ignored) {
                }
            }
        }

        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPassword = System.getenv("DB_PASSWORD");

        url = (envUrl != null && !envUrl.trim().isEmpty())
                ? envUrl.trim()
                : props.getProperty("db.url", DEFAULT_URL);

        user = (envUser != null && !envUser.trim().isEmpty())
                ? envUser.trim()
                : props.getProperty("db.user", DEFAULT_USER);

        password = (envPassword != null)
                ? envPassword
                : props.getProperty("db.password", DEFAULT_PASSWORD);

        initialized = true;
    }

    public static synchronized String getUrl() {
        init();
        return url;
    }

    public static synchronized String getUser() {
        init();
        return user;
    }

    public static synchronized String getPassword() {
        init();
        return password;
    }

    public static synchronized void setCredentials(String testUrl, String testUser, String testPassword) {
        url = testUrl;
        user = testUser;
        password = testPassword;
        initialized = true;
    }

    public static synchronized void reset() {
        initialized = false;
        url = null;
        user = null;
        password = null;
    }
}
