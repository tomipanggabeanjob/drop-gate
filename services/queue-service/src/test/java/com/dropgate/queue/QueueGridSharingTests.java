package com.dropgate.queue;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QueueGridSharingTests {

    @Test
    void queueTicketsAreVisibleAcrossServiceInstances() {
        Config config = new Config();
        config.setClusterName("queue-grid-sharing-test");
        config.getNetworkConfig().setPortAutoIncrement(true);
        config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        HazelcastInstance member = Hazelcast.newHazelcastInstance(config);
        String memberAddress = member.getCluster().getLocalMember().getAddress().getHost()
                + ":" + member.getCluster().getLocalMember().getAddress().getPort();
        HazelcastInstance firstClient = HazelcastClient.newHazelcastClient(clientConfig(memberAddress));
        HazelcastInstance secondClient = HazelcastClient.newHazelcastClient(clientConfig(memberAddress));

        try {
            RateLimitService rateLimitService =
                    new RateLimitService(new RateLimitProperties("token-bucket", 10, 5, 10));
            QueueAdmissionService first = new QueueAdmissionService(
                    rateLimitService, mock(ClickEventPublisher.class), firstClient);
            QueueAdmissionService second = new QueueAdmissionService(
                    rateLimitService, mock(ClickEventPublisher.class), secondClient);

            QueueAdmissionResponse admitted = first.admit(
                    "drop-grid", new QueueAdmissionRequest("drop-grid", "account-grid", "fingerprint-grid"));

            QueuePositionResponse position = second.position("drop-grid", "account-grid");

            assertThat(position.ticketId()).isEqualTo(admitted.token());
            assertThat(position.queuePosition()).isEqualTo(1);

            AdmissionToken admission = first.issueAdmissionToken("drop-grid", "account-grid", 30_000L);
            assertThat(second.activeAdmissions("drop-grid")).contains(admission);
        } finally {
            firstClient.shutdown();
            secondClient.shutdown();
            member.shutdown();
        }
    }

    private ClientConfig clientConfig(String address) {
        ClientConfig config = new ClientConfig();
        config.setClusterName("queue-grid-sharing-test");
        config.getNetworkConfig().addAddress(address);
        return config;
    }
}
