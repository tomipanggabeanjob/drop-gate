package com.dropgate.fraud;

import com.dropgate.events.ClickEvent;
import com.dropgate.events.FraudScoreEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class FraudScoringTopologyTests {

    @Test
    void scoresClickEventsAndPreservesFingerprintKey() {
        StreamsBuilder builder = new StreamsBuilder();
        new FraudScoringTopology().fraudScoringStream(builder, new FraudScoringService());

        Properties properties = new Properties();
        properties.put("application.id", "fraud-scoring-test");
        properties.put("bootstrap.servers", "localhost:9092");

        try (TopologyTestDriver driver = new TopologyTestDriver(builder.build(), properties)) {
            TestInputTopic<String, ClickEvent> input = driver.createInputTopic(
                    "click-events",
                    Serdes.String().serializer(),
                    new JsonSerde<>(ClickEvent.class).noTypeInfo().serializer());
            TestOutputTopic<String, FraudScoreEvent> output = driver.createOutputTopic(
                    "fraud-scores",
                    Serdes.String().deserializer(),
                    new JsonSerde<>(FraudScoreEvent.class).noTypeInfo().deserializer());

            input.pipeInput("fp-queue", new ClickEvent(
                    "event-1", "fp-queue", "drop-42", "acct-1", "queue_join", null, null, 1L, 1));

            KeyValue<String, FraudScoreEvent> result = output.readKeyValue();
            assertThat(result.key).isEqualTo("fp-queue");
            assertThat(result.value.fingerprintId()).isEqualTo("fp-queue");
            assertThat(result.value.source()).isEqualTo("queue_join");
            assertThat(result.value.score()).isEqualTo(82.0);
            assertThat(result.value.timestamp()).isPositive();
            assertThat(output.isEmpty()).isTrue();
        }
    }
}
