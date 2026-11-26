package com.dropgate.queue;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class HazelcastGridConfiguration {

    @Bean(destroyMethod = "shutdown")
    @Profile("!test & !local")
    public HazelcastInstance hazelcastClient(
            @Value("${dropgate.grid.address:localhost:5701}") String address) {
        ClientConfig config = new ClientConfig();
        config.setClusterName("dropgate");
        config.getNetworkConfig().addAddress(address);
        return HazelcastClient.newHazelcastClient(config);
    }

    @Bean(destroyMethod = "shutdown")
    @Profile({"test", "local"})
    public HazelcastInstance localHazelcastInstance() {
        Config config = new Config();
        config.setClusterName("dropgate-local");
        config.getNetworkConfig().setPortAutoIncrement(true);
        config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        return Hazelcast.newHazelcastInstance(config);
    }
}
