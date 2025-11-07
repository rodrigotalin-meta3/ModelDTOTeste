package br.com.meta3.java.scaffold.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds legacy DB connection settings from application properties.
 * 
 * Prefix: legacy.datasource
 * 
 * Properties expected:
 * legacy.datasource.tipobanco
 * legacy.datasource.urloracle
 * legacy.datasource.useroracle
 * legacy.datasource.passwordoracle
 * legacy.datasource.usersqlserver
 * legacy.datasource.passwordsqlserver
 * 
 * TODO: (REVIEW) Consider adding SQLServer port, serverName and databaseName if needed
 */
@Configuration
@ConfigurationProperties(prefix = "legacy.datasource")
public class LegacyDataSourceProperties {

    /**
     * Type of the legacy database. e.g. "oracle" or "sqlserver"
     */
    private String tipobanco;

    /**
     * JDBC URL for Oracle connections
     */
    private String urloracle;

    /**
     * Username for Oracle
     */
    private String useroracle;

    /**
     * Password for Oracle
     */
    private String passwordoracle;

    /**
     * Username for SQL Server
     */
    private String usersqlserver;

    /**
     * Password for SQL Server
     */
    private String passwordsqlserver;

    public String getTipobanco() {
        return tipobanco;
    }

    public void setTipobanco(String tipobanco) {
        this.tipobanco = tipobanco;
    }

    public String getUrloracle() {
        return urloracle;
    }

    public void setUrloracle(String urloracle) {
        this.urloracle = urloracle;
    }

    public String getUseroracle() {
        return useroracle;
    }

    public void setUseroracle(String useroracle) {
        this.useroracle = useroracle;
    }

    public String getPasswordoracle() {
        return passwordoracle;
    }

    public void setPasswordoracle(String passwordoracle) {
        this.passwordoracle = passwordoracle;
    }

    public String getUsersqlserver() {
        return usersqlserver;
    }

    public void setUsersqlserver(String usersqlserver) {
        this.usersqlserver = usersqlserver;
    }

    public String getPasswordsqlserver() {
        return passwordsqlserver;
    }

    public void setPasswordsqlserver(String passwordsqlserver) {
        this.passwordsqlserver = passwordsqlserver;
    }
}
