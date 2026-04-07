package com.assessment.securitystarter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.starter")
public class SecurityStarterProperties {

    private String jwtSecret;
    private long tokenExpirySeconds = 3600;
    private String tokenIssuer = "assessment-security-starter";

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenExpirySeconds() {
        return tokenExpirySeconds;
    }

    public void setTokenExpirySeconds(long tokenExpirySeconds) {
        this.tokenExpirySeconds = tokenExpirySeconds;
    }

    public String getTokenIssuer() {
        return tokenIssuer;
    }

    public void setTokenIssuer(String tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }
}
