/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.btm.config.service.rest.client;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.btm.api.model.config.btxn.ConfigMessage;
import org.hawkular.btm.api.services.ConfigurationLoader;
import org.hawkular.btm.api.services.ConfigurationService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the REST client implementation for the Configuration Service
 * API.
 *
 * @author gbrown
 */
public class ConfigurationServiceRESTClient implements ConfigurationService {

    private static final Logger log = Logger.getLogger(ConfigurationServiceRESTClient.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final TypeReference<java.util.List<BusinessTxnSummary>> BTXN_SUMMARY_LIST =
            new TypeReference<java.util.List<BusinessTxnSummary>>() {
    };

    private static final TypeReference<java.util.Map<String, BusinessTxnConfig>> BUSINESS_TXN_MAP =
            new TypeReference<java.util.Map<String, BusinessTxnConfig>>() {
    };

    private static final TypeReference<java.util.List<ConfigMessage>> CONFIG_MESSAGE_LIST =
            new TypeReference<java.util.List<ConfigMessage>>() {
    };

    private static final String HAWKULAR_PERSONA = "Hawkular-Persona";

    private String username = System.getProperty("hawkular-btm.username");
    private String password = System.getProperty("hawkular-btm.password");

    private String authorization = null;

    private String baseUrl;

    {
        baseUrl = System.getProperty("hawkular-btm.base-uri");

        if (baseUrl != null && baseUrl.length() > 0 && baseUrl.charAt(baseUrl.length() - 1) != '/') {
            baseUrl = baseUrl + '/';
        }
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;

        // Clear any previously computed authorization string
        this.authorization = null;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;

        // Clear any previously computed authorization string
        this.authorization = null;
    }

    /**
     * @return the baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @param baseUrl the baseUrl to set
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#getCollector(java.lang.String,
     *                                  java.lang.String, java.lang.String)
     */
    @Override
    public CollectorConfiguration getCollector(String tenantId, String host, String server) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get collector configuration: tenantId=[" + tenantId + "] host=[" + host
                    + "] server=[" + server + "]");
        }

        // Check if BTM configuration provided locally
        if (System.getProperty(ConfigurationLoader.HAWKULAR_BTM_CONFIG) != null) {
            return ConfigurationLoader.getConfiguration();
        }

        StringBuilder builder = new StringBuilder().append(baseUrl).append("config/collector");

        if (host != null) {
            builder.append("?host=");
            builder.append(host);
        }

        if (server != null) {
            if (host == null) {
                builder.append('?');
            } else {
                builder.append('&');
            }
            builder.append("server=");
            builder.append(server);
        }

        try {
            URL url = new URL(builder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            java.io.InputStream is = connection.getInputStream();

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                try {
                    return mapper.readValue(resp.toString(), CollectorConfiguration.class);
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Failed to deserialize", t);
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get collector configuration: status=[" + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send 'get' collector configuration request", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#updateBusinessTransaction(java.lang.String,
     *              java.lang.String, org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig)
     */
    @Override
    public List<ConfigMessage> updateBusinessTransaction(String tenantId, String name, BusinessTxnConfig config) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Update busioess transaction configuration: tenantId=[" + tenantId + "] name=[" + name
                    + "] config=[" + config + "]");
        }

        StringBuilder builder = new StringBuilder()
                .append(baseUrl)
                .append("config/businesstxn/full/")
                .append(name);

        try {
            URL url = new URL(builder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("PUT");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            java.io.OutputStream os = connection.getOutputStream();

            os.write(mapper.writeValueAsBytes(config));

            os.flush();
            os.close();

            java.io.InputStream is = connection.getInputStream();

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Update business transaction [" + name + "] configuration: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return mapper.readValue(resp.toString(), CONFIG_MESSAGE_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                log.severe("Failed to update business transaction [" + name + "] configuration: status=["
                        + connection.getResponseCode() + "]:"
                        + connection.getResponseMessage());
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to update business transaction  [" + name + "] configuration", e);
        }

        return Collections.emptyList();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#validateBusinessTransaction(
     *                  org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig)
     */
    @Override
    public List<ConfigMessage> validateBusinessTransaction(BusinessTxnConfig config) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Validate busioess transaction configuration: config=[" + config + "]");
        }

        StringBuilder builder = new StringBuilder()
                .append(baseUrl)
                .append("config/businesstxn/validate");

        try {
            URL url = new URL(builder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, null);

            java.io.OutputStream os = connection.getOutputStream();

            os.write(mapper.writeValueAsBytes(config));

            os.flush();
            os.close();

            java.io.InputStream is = connection.getInputStream();

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Validate business transaction configuration: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return mapper.readValue(resp.toString(), CONFIG_MESSAGE_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                log.severe("Failed to validate business transaction configuration: status=["
                        + connection.getResponseCode() + "]:"
                        + connection.getResponseMessage());
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to validate business transaction configuration", e);
        }

        return Collections.emptyList();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#getBusinessTransaction(java.lang.String,
     *                          java.lang.String)
     */
    @Override
    public BusinessTxnConfig getBusinessTransaction(String tenantId, String name) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get business transaction configuration: tenantId=[" + tenantId + "] name=["
                    + name + "]");
        }

        StringBuilder builder = new StringBuilder()
            .append(baseUrl)
            .append("config/businesstxn/full/")
            .append(name);

        try {
            URL url = new URL(builder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            java.io.InputStream is = connection.getInputStream();

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return mapper.readValue(resp.toString(), BusinessTxnConfig.class);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get business transaction [" + name + "] configuration: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get business transaction [" + name + "] configuration", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#getBusinessTransactionSummaries(java.lang.String)
     */
    @Override
    public List<BusinessTxnSummary> getBusinessTransactionSummaries(String tenantId) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get business transaction summaries: tenantId=[" + tenantId + "]");
        }

        StringBuilder builder = new StringBuilder()
            .append(baseUrl)
            .append("config/businesstxn/summary");

        try {
            URL url = new URL(builder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            java.io.InputStream is = connection.getInputStream();

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return mapper.readValue(resp.toString(), BTXN_SUMMARY_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get business transaction summaries: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get business transaction summaries", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#getBusinessTransactions(java.lang.String, long)
     */
    @Override
    public Map<String, BusinessTxnConfig> getBusinessTransactions(String tenantId, long updated) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get business transaction configurations: tenantId=[" + tenantId
                    + "] updated=[" + updated + "]");
        }

        StringBuilder builder = new StringBuilder()
            .append(baseUrl)
            .append("config/businesstxn/full?updated=")
            .append(updated);

        try {
            URL url = new URL(builder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            java.io.InputStream is = connection.getInputStream();

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return mapper.readValue(resp.toString(), BUSINESS_TXN_MAP);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get business transaction configurations: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get business transaction configurations", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#removeBusinessTransaction(java.lang.String,
     *                          java.lang.String)
     */
    @Override
    public void removeBusinessTransaction(String tenantId, String name) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Update busioess transaction configuration: tenantId=[" + tenantId + "] name=["
                    + name + "]]");
        }

        StringBuilder builder = new StringBuilder()
                .append(baseUrl)
                .append("config/businesstxn/full/")
                .append(name);

        try {
            URL url = new URL(builder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("DELETE");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            connection.getResponseCode();

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Remove business transaction [" + name + "] configuration: status=["
                        + connection.getResponseCode() + "]:"
                        + connection.getResponseMessage());
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to remove business transaction  [" + name + "] configuration", e);
        }
    }

    /**
     * Add the header values to the supplied connection.
     *
     * @param connection The connection
     * @param tenantId The optional tenant id
     */
    protected void addHeaders(HttpURLConnection connection, String tenantId) {
        if (tenantId != null) {
            connection.setRequestProperty(HAWKULAR_PERSONA, tenantId);
        }

        if (authorization == null && username != null) {
            String authString = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(authString.getBytes());

            authorization = "Basic " + encoded;
        }

        if (authorization != null) {
            connection.setRequestProperty("Authorization", authorization);
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Clear business transaction configurations: tenantId=[" + tenantId + "]");
        }

        try {
            URL url = new URL(new StringBuilder().append(getBaseUrl()).append("config").toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("DELETE");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Business transaction configs cleared");
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to clear business transaction configurations: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send 'clear' business transaction config request", e);
        }
    }

}
