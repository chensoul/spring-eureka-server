package com.chensoul.eureka.config;

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.configuration.SSLContextFactory;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"server.ssl.enabled"}, havingValue = "true")
public class EurekaClientTlsConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(EurekaClientTlsConfiguration.class);

    EurekaClientTlsConfiguration(TlsProperties tlsProperties, AbstractDiscoveryClientOptionalArgs<?> args) {
        if (tlsProperties.isEnabled()) {
            LOGGER.debug("TLS is enabled, configuring SSLContext for discovery client");
            args.setSSLContext(sslContext(tlsProperties));
        } else {
            LOGGER.debug("TLS is NOT enabled. configuring SSLContext for discovery client");
            args.setSSLContext(trustAllSslContext());
            args.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

    }

    private static SSLContext sslContext(TlsProperties tlsProperties) {
        try {
            return (new SSLContextFactory(tlsProperties)).createSSLContext();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Unexpected error during SSLContext creation!", e);
        }
    }

    private static SSLContext trustAllSslContext() {
        try {
            return (new SSLContextBuilder()).loadTrustMaterial(TrustAllStrategy.INSTANCE).build();
        } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Unexpected error during SSLContext creation!", e);
        }
    }
}