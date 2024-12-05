package com.chensoul.eureka.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class StaticPeerPostProcessor implements EnvironmentPostProcessor, InitializingBean {
    private static final DeferredLog LOG = new DeferredLog();

    StaticPeerPostProcessor() {
    }

    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String podName = environment.getProperty("pod.name");
        if (!StringUtils.hasText(podName)) {
            LOG.info("Not deployed by eureka controller? skipping post processor");
        } else {
            LOG.info(String.format("Pod name: %s", podName));
            int count = this.getIntValueFromEnvironment(environment, "pod.count", 1);
            LOG.info(String.format("Scale count: %d", count));
            String podNamePrefix = environment.getProperty("pod.name-prefix");
            LOG.info(String.format("Pod name prefix: %s", podNamePrefix));
            String serviceScheme = this.getServerScheme(environment);
            LOG.info(String.format("service scheme: %s", serviceScheme));
            String serviceName = environment.getProperty("service.domain");
            LOG.info(String.format("Service name: %s", serviceName));
            int servicePort = this.getIntValueFromEnvironment(environment, "server.port", 8080);
            LOG.info(String.format("Service port: %d", servicePort));
            String peers = environment.getProperty("peers");
            LOG.info(String.format("peers: %s", peers));
            PeerEnvironmentManager environmentManager = new PeerEnvironmentManager(environment, LOG);
            environmentManager.updateInstance(podName, serviceScheme, serviceName, servicePort, count);
            environmentManager.addScaledPeerUrls(podNamePrefix, serviceScheme, serviceName, servicePort, count);
            if (StringUtils.hasText(peers)) {
                environmentManager.addPeerUrls(peers);
            }

        }
    }

    public void afterPropertiesSet() {
        LOG.replayTo(this.getClass());
    }

    private int getIntValueFromEnvironment(ConfigurableEnvironment environment, String key, int defaultValue) {
        String integerString = environment.getProperty(key);
        if (!StringUtils.hasText(integerString)) {
            LOG.error(String.format("No %s provided, use default value %d", key, defaultValue));
            return defaultValue;
        } else {
            return Integer.parseInt(integerString);
        }
    }

    private String getServerScheme(ConfigurableEnvironment environment) {
        boolean sslEnabled = (Boolean)environment.getProperty("server.ssl.enabled", Boolean.class, false);
        return sslEnabled ? "https" : "http";
    }
}
