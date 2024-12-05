package com.chensoul.eureka.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

class PeerEnvironmentManager {
    private static final String PEER_MANAGER_PROPERTY_SOURCE = "PeerPropertySource";
    static final String REGISTER_WITH_EUREKA_PROPERTY_KEY = "eureka.client.register-with-eureka";
    static final String FETCH_REGISTRY_PROPERTY_KEY = "eureka.client.fetch-registry";
    static final String SERVICE_URLS_PROPERTY_KEY = "eureka.client.service-url.defaultZone";
    static final String MY_SERVICE_URL_PROPERTY_KEY = "eureka.server.my-url";
    static final String EUREKA_INSTANCE_PROPERTY_KEY = "eureka.instance.hostname";
    private static final Object lock = new Object();
    private final DeferredLog log;
    private final Map<String, Object> map;

    PeerEnvironmentManager(ConfigurableEnvironment environment, DeferredLog log) {
        this.log = log;
        if (!environment.getPropertySources().contains(PEER_MANAGER_PROPERTY_SOURCE)) {
            synchronized (lock) {
                if (!environment.getPropertySources().contains(PEER_MANAGER_PROPERTY_SOURCE)) {
                    environment.getPropertySources().addFirst(new MapPropertySource(PEER_MANAGER_PROPERTY_SOURCE, new ConcurrentHashMap()));
                }
            }
        }

        this.map = (Map) environment.getPropertySources().get(PEER_MANAGER_PROPERTY_SOURCE).getSource();
    }

    void addPeerUrls(String peers) {
        this.map.merge(SERVICE_URLS_PROPERTY_KEY, peers, (oldPeers, newPeers) ->
                String.join(",", oldPeers.toString(), newPeers.toString()));
        this.log.info("Current service urls are: " + this.map.get(SERVICE_URLS_PROPERTY_KEY));
    }

    void addScaledPeerUrls(String podNamePrefix, String serviceScheme, String serviceName, int servicePort, int count) {
        String scaledServiceUrls = IntStream.range(0, count).mapToObj((index) -> this.buildPodName(podNamePrefix, index))
                .map((podName) -> this.buildHostname(podName, serviceName))
                .map((hostName) -> this.buildServiceUrl(serviceScheme, hostName, servicePort))
                .collect(Collectors.joining(","));
        this.addPeerUrls(scaledServiceUrls);
    }

    void updateInstance(String podName, String serviceScheme, String serviceName, int servicePort, int count) {
        String hostname = this.buildHostname(podName, serviceName);
        this.map.put(EUREKA_INSTANCE_PROPERTY_KEY, hostname);
        this.map.put(MY_SERVICE_URL_PROPERTY_KEY, this.buildServiceUrl(serviceScheme, hostname, servicePort));
        if (count > 1) {
            this.map.put(REGISTER_WITH_EUREKA_PROPERTY_KEY, true);
            this.map.put(FETCH_REGISTRY_PROPERTY_KEY, true);
        } else {
            this.map.put(REGISTER_WITH_EUREKA_PROPERTY_KEY, false);
            this.map.put(FETCH_REGISTRY_PROPERTY_KEY, false);
        }
    }

    private String buildServiceUrl(String scheme, String hostname, int port) {
        return String.format("%s://%s:%d/eureka/", scheme, hostname, port);
    }

    private String buildPodName(String podNamePrefix, int index) {
        return String.format("%s-%d", podNamePrefix, index);
    }

    private String buildHostname(String podName, String serviceName) {
        return String.format("%s.%s", podName, serviceName);
    }
}
