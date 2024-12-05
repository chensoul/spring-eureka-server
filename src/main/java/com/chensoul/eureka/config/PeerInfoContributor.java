package com.chensoul.eureka.config;

import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.util.StatusUtil;
import java.util.Map;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class PeerInfoContributor implements InfoContributor {
    private final EurekaServerContext eurekaServerContext;

    public PeerInfoContributor(EurekaServerContext eurekaServerContext) {
        this.eurekaServerContext = eurekaServerContext;
    }

    public void contribute(Info.Builder builder) {
        Map<String, String> applicationStats = (new StatusUtil(this.eurekaServerContext)).getStatusInfo().getApplicationStats();
        builder.withDetail("peers", applicationStats);
    }
}
