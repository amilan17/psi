package org.cedar.psi.registry.stream

import groovy.util.logging.Slf4j
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.KGroupedStream
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import javax.xml.validation.Schema

@Slf4j
@Configuration
class RawGranuleStreamConfig {

  static final String topic = 'granule'

  static final String id = "raw-granule-aggregator"

  static final String storeName = 'raw-granules'
  static String outputTopic = "raw-granule-aggregator"

  @Value('${kafka.bootstrap.servers}')
  String bootstrapServers

  @Value('${kafka.schema.registry}')
  private String schemaRegistryUrl

  @Bean
  StreamsConfig rawGranuleConfig() {
    return new StreamsConfig([
        (StreamsConfig.APPLICATION_ID_CONFIG)           : id,
        (StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)        : bootstrapServers,
        (StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG)  : Serdes.String().class.name,
        (StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG): Serdes.String().class.name,
        (AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG): schemaRegistryUrl,
        (StreamsConfig.COMMIT_INTERVAL_MS_CONFIG)       : 500,
        (ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)       : "earliest"
    ])
  }

  @Bean
  Topology rawGranuleTopology() {
    def builder = new StreamsBuilder()
    // When you want to override serdes explicitly/selectively
    final Map<String, String> serdeConfig = Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)

//    final Serde<GenericRecord> keyGenericAvroSerde = new GenericAvroSerde()
//    keyGenericAvroSerde.configure(serdeConfig, true) // `true` for record keys
    final Serde<GenericRecord> valueGenericAvroSerde = new GenericAvroSerde()
    valueGenericAvroSerde.configure(serdeConfig, false) // `false` for record values

    KStream rawStream = builder.stream(topic)
    KStream transformedStream = rawStream.mapValues({ it -> CreateSchema.reduceValue(it as String) })
    KGroupedStream groupedStream = transformedStream.groupByKey()
    KTable mergedGranules = groupedStream.reduce(
            GranuleFunctions.mergeGranules,
            Materialized.as(storeName).withValueSerde(valueGenericAvroSerde))

    return builder.build()
  }


  @Bean(initMethod = 'start', destroyMethod = 'close')
  KafkaStreams rawGranuleStream(Topology rawGranuleTopology, StreamsConfig rawGranuleConfig) {
    return new KafkaStreams(rawGranuleTopology, rawGranuleConfig)
  }

}
