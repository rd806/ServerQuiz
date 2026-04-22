package org.rd806.serverquiz.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceManager {
    // 创建连接池
    private HikariDataSource dataSource;
    private final DatabaseConfig databaseConfig;

    public DataSourceManager(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        setupDataSource();
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();
        // 设置最大连接池大小
        config.setMaximumPoolSize(10);
        // 保持5个空闲连接池可用
        config.setMinimumIdle(5);
        // 空闲时间超过30秒则关闭
        config.setIdleTimeout(30000);
        // 设置连接参数
        config.setJdbcUrl(databaseConfig.getJdbcUrl());
        config.setUsername(databaseConfig.getUsername());
        config.setPassword(databaseConfig.getPassword());

        // 开启客户端预处理语句缓存
        config.addDataSourceProperty("cachePrepStmts", "true");
        // 设置最多缓存 250 个预处理语句对象
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        // 缓存语句的 SQL 长度限制 2048 个字符
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    // 获取连接
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // 取消连接
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
