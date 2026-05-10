package com.example.performanceTesting.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class DatabaseBootstrapProperties {

    private final Database database = new Database();
    private final Seed seed = new Seed();

    public Database getDatabase() {
        return database;
    }

    public Seed getSeed() {
        return seed;
    }

    public static class Database {
        private DatabaseType type;
        private final Jdbc postgres = new Jdbc(
                "jdbc:postgresql://localhost:5432/performance_test",
                "postgres",
                "password",
                "org.postgresql.Driver");
        private final Jdbc mysql = new Jdbc(
                "jdbc:mysql://localhost:3306/performance_test?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&rewriteBatchedStatements=true&allowLoadLocalInfile=true",
                "app",
                "password",
                "com.mysql.cj.jdbc.Driver");
        private final Mongo mongo = new Mongo();
        private final Couchbase couchbase = new Couchbase();

        public DatabaseType getType() {
            if (type == null) {
                throw new IllegalStateException(
                        "Database type must be explicitly configured via 'app.database.type' property (case-insensitive). " +
                        "Valid values: postgres, mysql, mongo, couchbase");
            }
            return type;
        }

        public void setType(DatabaseType type) {
            this.type = type;
        }

        public Jdbc getPostgres() {
            return postgres;
        }

        public Jdbc getMysql() {
            return mysql;
        }

        public Couchbase getCouchbase() {
            return couchbase;
        }

        public Mongo getMongo() {
            return mongo;
        }
    }

    public static class Mongo {
        private String uri = "mongodb://localhost:27017";
        private String database = "performance_test";
        private boolean indexes = true;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public boolean isIndexes() {
            return indexes;
        }

        public void setIndexes(boolean indexes) {
            this.indexes = indexes;
        }
    }

    public static class Couchbase {
        private String connectionString = "couchbase://localhost";
        private String username = "Administrator";
        private String password = "password";
        private String bucket = "perf_bucket";
        private String scope = "shop";
        private boolean indexes = true;

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public boolean isIndexes() {
            return indexes;
        }

        public void setIndexes(boolean indexes) {
            this.indexes = indexes;
        }
    }

    public static class Jdbc {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private boolean indexes = true;

        public Jdbc() {
        }

        public Jdbc(String url, String username, String password, String driverClassName) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.driverClassName = driverClassName;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public boolean isIndexes() {
            return indexes;
        }

        public void setIndexes(boolean indexes) {
            this.indexes = indexes;
        }
    }

    public static class Seed {
        private boolean enabled = true;
        private String dataset = "1_000_000";
        private String location = "classpath*:database";
        private int batchSize = 5_000;
        private boolean resetBeforeLoad = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDataset() {
            return dataset;
        }

        public void setDataset(String dataset) {
            this.dataset = dataset;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public boolean isResetBeforeLoad() {
            return resetBeforeLoad;
        }

        public void setResetBeforeLoad(boolean resetBeforeLoad) {
            this.resetBeforeLoad = resetBeforeLoad;
        }
    }
}

