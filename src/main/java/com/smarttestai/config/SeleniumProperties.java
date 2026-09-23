package com.smarttestai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "selenium")
public class SeleniumProperties {

    private String baseUrl = "http://localhost:8080";
    private String defaultBrowser = "CHROME";
    private boolean headless = true;
    private int implicitWaitSeconds = 10;
    private int pageLoadTimeoutSeconds = 30;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDefaultBrowser() {
        return defaultBrowser;
    }

    public void setDefaultBrowser(String defaultBrowser) {
        this.defaultBrowser = defaultBrowser;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public int getImplicitWaitSeconds() {
        return implicitWaitSeconds;
    }

    public void setImplicitWaitSeconds(int implicitWaitSeconds) {
        this.implicitWaitSeconds = implicitWaitSeconds;
    }

    public int getPageLoadTimeoutSeconds() {
        return pageLoadTimeoutSeconds;
    }

    public void setPageLoadTimeoutSeconds(int pageLoadTimeoutSeconds) {
        this.pageLoadTimeoutSeconds = pageLoadTimeoutSeconds;
    }
}
