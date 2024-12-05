package com.chensoul.eureka.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey3.EurekaIdentityHeaderFilter;
import com.netflix.discovery.shared.transport.jersey3.EurekaJersey3Client;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerIdentity;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.Jersey3DynamicGZIPContentEncodingFilter;
import com.netflix.eureka.transport.Jersey3ReplicationClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.net.ssl.SSLContext;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.eureka.server.ReplicationClientAdditionalFilters;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(name = {"server.ssl.enabled"}, havingValue = "true")
public class EurekaTlsEnabledPeerConfiguration {
    private final EurekaServerConfig eurekaServerConfig;
    private final EurekaClientConfig eurekaClientConfig;
    private final ApplicationInfoManager applicationInfoManager;
    private final Environment env;

    public EurekaTlsEnabledPeerConfiguration(EurekaServerConfig eurekaServerConfig, EurekaClientConfig eurekaClientConfig, ApplicationInfoManager applicationInfoManager, Environment env) {
        this.eurekaServerConfig = eurekaServerConfig;
        this.eurekaClientConfig = eurekaClientConfig;
        this.applicationInfoManager = applicationInfoManager;
        this.env = env;
    }

    @Bean
    public PeerEurekaNodes peerEurekaNodes(PeerAwareInstanceRegistry registry, ServerCodecs serverCodecs, ReplicationClientAdditionalFilters replicationClientAdditionalFilters) {
        String keystore = this.env.getProperty("eureka.client.tls.key-store");
        String keystorePassword = this.env.getProperty("eureka.client.tls.key-store-password");
        String truststore = this.env.getProperty("eureka.client.tls.trust-store");
        String truststorePassword = this.env.getProperty("eureka.client.tls.trust-store-password");
        String clientAuth = this.env.getProperty("server.ssl.client-auth");
        return new SSLEnabledRefreshablePeerEurekaNodes(registry, this.eurekaServerConfig, this.eurekaClientConfig, serverCodecs, this.applicationInfoManager, replicationClientAdditionalFilters, clientAuth, keystore, keystorePassword, truststore, truststorePassword);
    }

    static class SSLEnabledRefreshablePeerEurekaNodes extends PeerEurekaNodes implements ApplicationListener<EnvironmentChangeEvent> {
        private static final Logger LOG = LoggerFactory.getLogger(SSLEnabledRefreshablePeerEurekaNodes.class);
        private final String clientAuth;
        private final String keystore;
        private final String keystorePassword;
        private final String truststore;
        private final String truststorePassword;
        ReplicationClientAdditionalFilters replicationClientAdditionalFilters;

        SSLEnabledRefreshablePeerEurekaNodes(final PeerAwareInstanceRegistry registry, final EurekaServerConfig serverConfig, final EurekaClientConfig clientConfig, final ServerCodecs serverCodecs, final ApplicationInfoManager applicationInfoManager, final ReplicationClientAdditionalFilters replicationClientAdditionalFilters, String clientAuth, String keystore, String keystorePassword, String truststore, String truststorePassword) {
            super(registry, serverConfig, clientConfig, serverCodecs, applicationInfoManager);
            this.replicationClientAdditionalFilters = replicationClientAdditionalFilters;
            this.clientAuth = clientAuth;
            this.keystore = keystore;
            this.keystorePassword = keystorePassword;
            this.truststore = truststore;
            this.truststorePassword = truststorePassword;
        }

        protected PeerEurekaNode createPeerEurekaNode(String peerEurekaNodeUrl) {
            Jersey3ReplicationClient replicationClient = createReplicationClient(this.serverConfig, this.serverCodecs, peerEurekaNodeUrl, this.replicationClientAdditionalFilters.getFilters(), this.clientAuth, this.keystore, this.keystorePassword, this.truststore, this.truststorePassword);
            String targetHost = hostFromUrl(peerEurekaNodeUrl);
            if (targetHost==null) {
                targetHost = "host";
            }

            return new PeerEurekaNode(this.registry, targetHost, peerEurekaNodeUrl, replicationClient, this.serverConfig);
        }

        private static Jersey3ReplicationClient createReplicationClient(EurekaServerConfig config, ServerCodecs serverCodecs, String serviceUrl, Collection<ClientRequestFilter> additionalFilters, String clientAuth, String keystore, String keystorePassword, String truststore, String truststorePassword) {
            String var10000 = Jersey3ReplicationClient.class.getSimpleName();
            String name = var10000 + ": " + serviceUrl + "apps/: ";

            EurekaJersey3Client jerseyClient;
            String ip;
            try {
                try {
                    ip = (new URL(serviceUrl)).getHost();
                } catch (MalformedURLException var16) {
                    ip = serviceUrl;
                }

                String jerseyClientName = "Discovery-PeerNodeClient-" + ip;
                MutualTlsEurekaJersey3ClientBuilder clientBuilder = (new MutualTlsEurekaJersey3ClientBuilder()).withClientName(jerseyClientName).withUserAgent("Java-EurekaClient-Replication").withEncoderWrapper(serverCodecs.getFullJsonCodec()).withDecoderWrapper(serverCodecs.getFullJsonCodec()).withConnectionTimeout(config.getPeerNodeConnectTimeoutMs()).withReadTimeout(config.getPeerNodeReadTimeoutMs()).withMaxConnectionsPerHost(config.getPeerNodeTotalConnectionsPerHost()).withMaxTotalConnections(config.getPeerNodeTotalConnections()).withConnectionIdleTimeout(config.getPeerNodeConnectionIdleTimeoutSeconds());
                SSLContext sslContext;
                if ("need".equalsIgnoreCase(clientAuth) && StringUtils.hasText(keystore)) {
                    LOG.debug("server.ssl.client-auth is set to need. Jersey client will use keystore from {}", keystore);
                    sslContext = (new SSLContextBuilder()).setProtocol("SSL").loadTrustMaterial(new URL(truststore), truststorePassword.toCharArray()).loadKeyMaterial(new URL(keystore), keystorePassword.toCharArray(), "".toCharArray(), (x, y) -> {
                        return "eureka";
                    }).build();
                    clientBuilder = clientBuilder.withCustomSSLContext(sslContext);
                } else {
                    LOG.debug("server.ssl.client-auth has not been set to need. Will configure Jersey Client to skip tls verification");
                    sslContext = (new SSLContextBuilder()).loadTrustMaterial((x, y) -> {
                        return true;
                    }).build();
                    clientBuilder = clientBuilder.withCustomSSLContext(sslContext);
                }

                jerseyClient = clientBuilder.build();
            } catch (Throwable e) {
                throw new RuntimeException("Cannot Create new Replica Node :" + name, e);
            }

            ip = null;

            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                LOG.warn("Cannot find localhost ip", e);
            }

            Client jerseyApacheClient = jerseyClient.getClient();
            jerseyApacheClient.register(new Jersey3DynamicGZIPContentEncodingFilter(config));

            additionalFilters.forEach(jerseyApacheClient::register);

            EurekaServerIdentity identity = new EurekaServerIdentity(ip);
            jerseyApacheClient.register(new EurekaIdentityHeaderFilter(identity));
            return new Jersey3ReplicationClient(jerseyClient, serviceUrl);
        }

        public void onApplicationEvent(final EnvironmentChangeEvent event) {
            if (this.shouldUpdate(event.getKeys())) {
                this.updatePeerEurekaNodes(this.resolvePeerUrls());
            }

        }

        protected boolean shouldUpdate(final Set<String> changedKeys) {
            assert changedKeys!=null;

            if (this.clientConfig.shouldUseDnsForFetchingServiceUrls()) {
                return false;
            } else if (changedKeys.contains("eureka.client.region")) {
                return true;
            } else {
                Iterator var2 = changedKeys.iterator();

                String key;
                do {
                    if (!var2.hasNext()) {
                        return false;
                    }

                    key = (String) var2.next();
                } while (!key.startsWith("eureka.client.service-url.") && !key.startsWith("eureka.client.availability-zones."));

                return true;
            }
        }
    }
}
