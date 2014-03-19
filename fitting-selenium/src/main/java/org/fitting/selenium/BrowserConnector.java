/*
 * Licensed to the Fitting Project under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Fitting Project licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.fitting.selenium;

import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

/** Selenium browser object. */
public class BrowserConnector {
    /** The underlying WebDriver implementation. */
    private WebDriver webDriver;
    /** Flag indicating if javascript is enabled. */
    private boolean javascriptEnabled;

    /**
     * Create a new Browser instance.
     *
     * @param capabilities The desired browser capabilities.
     * @param url          The URL to connect to.
     */
    private BrowserConnector(DesiredCapabilities capabilities, URL url) {
        webDriver = new RemoteWebDriver(url, capabilities);
        javascriptEnabled = capabilities.isJavascriptEnabled();
    }

    /**
     * Get the underlying WebDriver implementation.
     *
     * @return The WebDriver.
     */
    public WebDriver getWebDriver() {
        return webDriver;
    }

    public SeleniumWindow getWindow() {
        return new SeleniumWindow("_MAIN", null, webDriver);
    }

    /**
     * Check if JavaScript is enabled.
     *
     * @return <code>true</code> if javascript is enabled.
     */
    public boolean isJavascriptEnabled() {
        return javascriptEnabled;
    }

    /** Destroy the Browser object. */
    public void destroy() {
        this.webDriver.close();
        this.webDriver = null;
        this.javascriptEnabled = false;
    }

    /**
     * Get a new builder for the Browser.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for creating Browser objects. */
    public static final class Builder {
        /** The URL for connecting to Selenium. */
        private static final String SELENIUM_CONNECTION_URL = "http://%s:%d/wd/hub";
        /** The URL for proxy servers, used by selenium. */
        private static final String SELENIUM_PROXY_URL = "%s:%d";
        /** The targeted platform. */
        private String platform;
        /** The targeted browser name. */
        private String browser;
        /** The targeted browser version. */
        private String version;
        /** The host the selenium server runs on. */
        private String host;
        /** The port the selenium server runs on. */
        private int port;
        /** Indicator javascript. */
        private boolean javascript = true;
        /** The added capabilities. */
        private final Map<String, Object> capabilities = new HashMap<String, Object>();

        /**
         * Set the platform the browser should run on.
         *
         * @param platform The platform name.
         *
         * @return The builder with the platform set.
         */
        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        /**
         * Set the browser to use.
         *
         * @param browser The name of the browser.
         * @param version The version of the browser.
         *
         * @return The builder with the browser set.
         *
         * @see Browser
         */
        public Builder withBrowser(String browser, String version) {
            this.browser = browser;
            this.version = version;
            return this;
        }

        /**
         * Set the browser to use.
         *
         * @param browser The name of the browser.
         *
         * @return The builder with the browser set.
         *
         * @see Browser
         */
        public Builder withBrowser(String browser) {
            this.browser = browser;
            return this;
        }

        /**
         * Set the host the selenium server is running on.
         *
         * @param host The host.
         * @param port The port.
         *
         * @return The builder with the selenium server host set.
         */
        public Builder onHost(String host, int port) {
            this.host = host;
            this.port = port;
            return this;
        }

        /**
         * Set the platform the selenium server is running on.
         *
         * @param platform The platform.
         *
         * @return The builder with the selenium server platform set.
         */
        public Builder onPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        /**
         * Set javascript support.
         *
         * @param javascriptEnabled Flag indicating javascript support.
         *
         * @return The builder with javascript support set.
         */
        public Builder withJavascriptEnabled(boolean javascriptEnabled) {
            this.javascript = javascriptEnabled;
            return this;
        }

        /**
         * Add a custom capability.
         *
         * @param capability The capability.
         * @param value      The capability value.
         *
         * @return The builder with the added capability.
         */
        public Builder withCapabilities(String capability, Object value) {
            if (!isEmpty(capability)) {
                this.capabilities.put(capability, value);
            }
            return this;
        }

        /**
         * Build the {@link org.fitting.selenium.BrowserConnector} with the set properties.
         *
         * @return The build browser connector.
         *
         * @throws IllegalArgumentException When invalid data was provided.
         */
        public BrowserConnector build() throws IllegalArgumentException {
            DesiredCapabilities capabilities = Browser.getBrowserForAlias(browser).createDesiredCapabilities();
            if (!isEmpty(version)) {
                capabilities.setCapability(CapabilityType.VERSION, version);
            }
            if (!isEmpty(platform)) {
                capabilities.setPlatform(Platform.extractFromSysProperty(platform));
            }
            capabilities.setJavascriptEnabled(javascript);
            capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
            for (String capability : this.capabilities.keySet()) {
                capabilities.setCapability(capability, this.capabilities.get(capability));
            }
            return new BrowserConnector(capabilities, createSeleniumUrl());
        }

        /**
         * Create the URL for the selenium server.
         *
         * @return The URL.
         *
         * @throws IllegalArgumentException When the URL is not valid.
         */
        private URL createSeleniumUrl() throws IllegalArgumentException {
            String url = format(SELENIUM_CONNECTION_URL, host, port);
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Unable to construct selenium url " + url, e);
            }
        }
    }
}
