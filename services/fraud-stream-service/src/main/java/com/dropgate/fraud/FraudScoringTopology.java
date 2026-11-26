package com.dropgate.fraud;

import com.dropgate.events.ClickEvent;
import com.dropgate.events.FraudScoreEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@EnableKafkaStreams
public class FraudScoringTopology {

    // Kafka Streams fails at startup if a source topic is missing, so the topics are declared explicitly.
    @Bean
    public NewTopic clickEventsTopic() {
        return TopicBuilder.name("click-events").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic fraudScoresTopic() {
        return TopicBuilder.name("fraud-scores").partitions(1).replicas(1).build();
    }

    @Bean
    public KStream<String, ClickEvent> fraudScoringStream(
            StreamsBuilder streamsBuilder,
            FraudScoringService fraudScoringService) {
        JsonSerde<ClickEvent> clickEventSerde = new JsonSerde<>(ClickEvent.class).noTypeInfo();
        JsonSerde<FraudScoreEvent> fraudScoreSerde = new JsonSerde<>(FraudScoreEvent.class).noTypeInfo();

        KStream<String, ClickEvent> clickEvents = streamsBuilder.stream(
                "click-events",
                Consumed.with(Serdes.String(), clickEventSerde));

        clickEvents
                .filter((key, event) -> event != null
                        && event.fingerprintId() != null
                        && !event.fingerprintId().isBlank()
                        && event.action() != null)
                .mapValues(fraudScoringService::score)
                .to("fraud-scores", Produced.with(Serdes.String(), fraudScoreSerde));

        return clickEvents;
    }
}
