package com.chensoul.eureka.config;

import com.netflix.discovery.converters.wrappers.CodecWrappers;
import com.netflix.discovery.converters.wrappers.DecoderWrapper;
import com.netflix.discovery.converters.wrappers.EncoderWrapper;
import com.netflix.discovery.provider.DiscoveryJerseyProvider;
import com.netflix.discovery.shared.transport.jersey3.EurekaJersey3Client;
import com.netflix.discovery.shared.transport.jersey3.EurekaJersey3ClientImpl;
import com.netflix.discovery.util.DiscoveryBuildInfo;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.client.ClientConfig;

public class MutualTlsEurekaJersey3ClientBuilder {
    private static final String PROTOCOL = "https";
    private static final String PROTOCOL_SCHEME = "SSL";
    private static final int HTTPS_PORT = 443;
    private static final String KEYSTORE_TYPE = "JKS";
    private boolean systemSSL;
    private String clientName;
    private int maxConnectionsPerHost;
    private int maxTotalConnections;
    private String trustStoreFileName;
    private String trustStorePassword;
    private SSLContext sslContext;
    private String userAgent;
    private String proxyUserName;
    private String proxyPassword;
    private String proxyHost;
    private String proxyPort;
    private int connectionTimeout;
    private int readTimeout;
    private int connectionIdleTimeout;
    private EncoderWrapper encoderWrapper;
    private DecoderWrapper decoderWrapper;

    public MutualTlsEurekaJersey3ClientBuilder() {
    }

    public MutualTlsEurekaJersey3ClientBuilder withClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withConnectionIdleTimeout(int connectionIdleTimeout) {
        this.connectionIdleTimeout = connectionIdleTimeout;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withProxy(String proxyHost, String proxyPort, String user, String password) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUserName = user;
        this.proxyPassword = password;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withSystemSSLConfiguration() {
        this.systemSSL = true;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withTrustStoreFile(String trustStoreFileName, String trustStorePassword) {
        this.trustStoreFileName = trustStoreFileName;
        this.trustStorePassword = trustStorePassword;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withCustomSSLContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withEncoder(String encoderName) {
        return this.withEncoderWrapper(CodecWrappers.getEncoder(encoderName));
    }

    public MutualTlsEurekaJersey3ClientBuilder withEncoderWrapper(EncoderWrapper encoderWrapper) {
        this.encoderWrapper = encoderWrapper;
        return this;
    }

    public MutualTlsEurekaJersey3ClientBuilder withDecoder(String decoderName, String clientDataAccept) {
        return this.withDecoderWrapper(CodecWrappers.resolveDecoder(decoderName, clientDataAccept));
    }

    public MutualTlsEurekaJersey3ClientBuilder withDecoderWrapper(DecoderWrapper decoderWrapper) {
        this.decoderWrapper = decoderWrapper;
        return this;
    }

    public EurekaJersey3Client build() {
        MyDefaultApacheHttpClient4Config config = new MyDefaultApacheHttpClient4Config();

        try {
            return new EurekaJersey3ClientImpl(this.connectionTimeout, this.readTimeout, this.connectionIdleTimeout, config);
        } catch (Throwable e) {
            throw new RuntimeException("Cannot create Jersey client ", e);
        }
    }

    class MyDefaultApacheHttpClient4Config extends ClientConfig {
        MyDefaultApacheHttpClient4Config() {
            PoolingHttpClientConnectionManager cm;
            if (MutualTlsEurekaJersey3ClientBuilder.this.sslContext!=null) {
                cm = this.createCustomSSLContextCM();
            } else if (MutualTlsEurekaJersey3ClientBuilder.this.systemSSL) {
                cm = this.createSystemSslCM();
            } else if (MutualTlsEurekaJersey3ClientBuilder.this.trustStoreFileName!=null && !MutualTlsEurekaJersey3ClientBuilder.this.trustStoreFileName.trim().isEmpty()) {
                cm = this.createCustomSslCM();
            } else {
                cm = new PoolingHttpClientConnectionManager();
            }

            if (MutualTlsEurekaJersey3ClientBuilder.this.proxyHost!=null) {
                this.addProxyConfiguration();
            }

            DiscoveryJerseyProvider discoveryJerseyProvider = new DiscoveryJerseyProvider(MutualTlsEurekaJersey3ClientBuilder.this.encoderWrapper, MutualTlsEurekaJersey3ClientBuilder.this.decoderWrapper);
            this.register(discoveryJerseyProvider);
            cm.setDefaultMaxPerRoute(MutualTlsEurekaJersey3ClientBuilder.this.maxConnectionsPerHost);
            cm.setMaxTotal(MutualTlsEurekaJersey3ClientBuilder.this.maxTotalConnections);
            this.property("jersey.config.apache.client.connectionManager", cm);
            String var10000 = MutualTlsEurekaJersey3ClientBuilder.this.userAgent==null ? MutualTlsEurekaJersey3ClientBuilder.this.clientName:MutualTlsEurekaJersey3ClientBuilder.this.userAgent;
            String fullUserAgentName = var10000 + "/v" + DiscoveryBuildInfo.buildVersion();
            this.property("http.useragent", fullUserAgentName);
            this.property("jersey.config.client.followRedirects", Boolean.FALSE);
            this.property("http.protocol.handle-redirects", Boolean.FALSE);
        }

        private void addProxyConfiguration() {
            if (MutualTlsEurekaJersey3ClientBuilder.this.proxyUserName!=null && MutualTlsEurekaJersey3ClientBuilder.this.proxyPassword!=null) {
                this.property("jersey.config.client.proxy.username", MutualTlsEurekaJersey3ClientBuilder.this.proxyUserName);
                this.property("jersey.config.client.proxy.password", MutualTlsEurekaJersey3ClientBuilder.this.proxyPassword);
            } else {
                this.property("jersey.config.client.proxy.username", "guest");
                this.property("jersey.config.client.proxy.password", "guest");
            }

            this.property("jersey.config.client.proxy.uri", "http://" + MutualTlsEurekaJersey3ClientBuilder.this.proxyHost + ":" + MutualTlsEurekaJersey3ClientBuilder.this.proxyPort);
        }

        private PoolingHttpClientConnectionManager createSystemSslCM() {
            ConnectionSocketFactory socketFactory = SSLConnectionSocketFactory.getSystemSocketFactory();
            Registry registry = RegistryBuilder.create().register(PROTOCOL, socketFactory).build();
            return new PoolingHttpClientConnectionManager(registry);
        }

        private PoolingHttpClientConnectionManager createCustomSSLContextCM() {
            try {
                ConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(MutualTlsEurekaJersey3ClientBuilder.this.sslContext, new NoopHostnameVerifier());
                Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register(PROTOCOL, socketFactory).build();
                return new PoolingHttpClientConnectionManager(registry);
            } catch (Exception ex) {
                throw new IllegalStateException("SSL configuration issue", ex);
            }
        }

        private PoolingHttpClientConnectionManager createCustomSslCM() {
            FileInputStream fin = null;

            PoolingHttpClientConnectionManager var8;
            try {
                SSLContext sslContext = SSLContext.getInstance(PROTOCOL_SCHEME);
                KeyStore sslKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
                fin = new FileInputStream(MutualTlsEurekaJersey3ClientBuilder.this.trustStoreFileName);
                sslKeyStore.load(fin, MutualTlsEurekaJersey3ClientBuilder.this.trustStorePassword.toCharArray());
                TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                factory.init(sslKeyStore);
                TrustManager[] trustManagers = factory.getTrustManagers();
                sslContext.init(null, trustManagers, (SecureRandom) null);
                ConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                Registry registry = RegistryBuilder.create().register(PROTOCOL, socketFactory).build();
                var8 = new PoolingHttpClientConnectionManager(registry);
            } catch (Exception ex) {
                throw new IllegalStateException("SSL configuration issue", ex);
            } finally {
                if (fin!=null) {
                    try {
                        fin.close();
                    } catch (IOException var16) {
                    }
                }
            }
            return var8;
        }
    }
}
