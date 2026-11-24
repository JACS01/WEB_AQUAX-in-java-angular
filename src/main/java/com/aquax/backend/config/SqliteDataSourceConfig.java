package com.aquax.backend.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.Function;
import org.sqlite.SQLiteConnection;

import com.zaxxer.hikari.HikariDataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "org.sqlite.JDBC")
public class SqliteDataSourceConfig {

    @Bean(destroyMethod = "close")
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource dataSource(DataSourceProperties propiedades) {
        HikariDataSource fuente = new SqliteHikariDataSource();
        fuente.setJdbcUrl(propiedades.determineUrl());
        fuente.setDriverClassName(propiedades.determineDriverClassName());
        fuente.setUsername(propiedades.determineUsername());
        fuente.setPassword(propiedades.determinePassword());
        return fuente;
    }

    private static final class SqliteHikariDataSource extends HikariDataSource {

        @Override
        public Connection getConnection() throws SQLException {
            Connection conexion = super.getConnection();
            try {
                Function.create(conexion.unwrap(SQLiteConnection.class), "lower", new Function() {
                    @Override
                    protected void xFunc() throws SQLException {
                        String texto = value_text(0);
                        result(texto == null ? null : texto.toLowerCase(Locale.ROOT));
                    }
                }, 1, Function.FLAG_DETERMINISTIC);
                return conexion;
            } catch (SQLException | RuntimeException error) {
                try {
                    conexion.close();
                } catch (SQLException cierre) {
                    error.addSuppressed(cierre);
                }
                throw error;
            }
        }
    }
}
