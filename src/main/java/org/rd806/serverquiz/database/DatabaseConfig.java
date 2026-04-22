package org.rd806.serverquiz.database;

public class DatabaseConfig {
    // 网络参数
    private String host;
    private int port;
    // 数据库参数
    private String database;
    private String username;
    private String password;
    private boolean useSSL;

    // 默认构造函数
    public DatabaseConfig() {
        this.host = "localhost";
        this.port = 3306;
        this.database = "QuizPlugin";
        this.username = "root";
        this.password = "";
        this.useSSL = false;
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isUseSSL() { return useSSL; }
    public void setUseSSL(boolean useSSL) { this.useSSL = useSSL; }

    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC",
                host, port, database, useSSL);
    }

}
