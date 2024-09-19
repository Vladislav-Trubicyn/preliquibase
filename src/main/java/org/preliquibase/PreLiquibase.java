package org.preliquibase;

import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreLiquibase implements BeanPostProcessor {
    @Value("${spring.liquibase.url:}")
    private String jdbcUrl;
    @Value("${spring.liquibase.driver-class-name:}")
    private String driverClass;
    @Value("${spring.liquibase.user:}")
    private String user;
    @Value("${spring.liquibase.password:}")
    private String password;

    private boolean isDone;

    private Logger log = LoggerFactory.getLogger(PreLiquibase.class);

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (!isDone) {
            String[] splitJdbcUrl = jdbcUrl.split("/");

            if(splitJdbcUrl.length < 4) {
                throw new RuntimeException("Unable to parse URL " + jdbcUrl);
            }

            String jdbcUrlWithoutDatabase = String.format("%s//%s/", splitJdbcUrl[0], splitJdbcUrl[2]);
            String dataBaseName = splitJdbcUrl[3];
            DriverManagerDataSource dataSource = new DriverManagerDataSource(jdbcUrlWithoutDatabase, user, password);
            if(StringUtils.isNotEmpty(driverClass)) {
                dataSource.setDriverClassName(driverClass);
            }

            try (Connection conn = dataSource.getConnection();
                 Statement statement = conn.createStatement()) {
                log.info("Going to create DB '{}' if not exists.", dataBaseName);
                statement.execute("create database " + dataBaseName);
            } catch (SQLException e) {
                if(!e.getSQLState().equals("42P04")){
                    throw new RuntimeException("Failed to create database '" + dataBaseName + "'", e);
                }
            }
            isDone = true;
        }
        return bean;
    }
}
