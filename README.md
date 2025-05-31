# spring-kafka-streams-demo
A lightweight Spring Boot application demonstrating how to configure Kafka Streams for testing. 
Includes examples of various testing strategies.

# Overview

### Topology Configuration
A topology for a very rudimentary card transaction fraud detection is defines in 
[FraudDetectionTopologyConfig](src/main/kotlin/no/roar/kafka/streams/config/FraudDetectionTopologyConfig.kt).

### Testing Approaches

#### 1. Unit Test with `TopologyTestDriver`  
Tests the topology in isolation using Kafka Streams' `TopologyTestDriver`.
See: [FraudDetectionTopologyTestDriverTest](src/test/kotlin/no/roar/kafka/streams/config/FraudDetectionTopologyTestDriverTest.kt).

#### 2. Message Validation with KafkaTestUtils  
Verifies that messages produced by the topology are correctly published to the output topic. This approach uses a consumer, `KafkaTestUtils`, and `@EmbeddedKafka`.
See: [FraudDetectionKafkaTestUtilsTest](src/test/kotlin/no/roar/kafka/streams/config/FraudDetectionKafkaTestUtilsTest.kt).

#### 3. Integration Testing with `@MockitoBean` and `@EmbeddedKafka  `
Validates that the expected service methods are invoked during the integration test.
See: [FraudDetectionIntegrationTest](src/test/kotlin/no/roar/kafka/streams/config/FraudDetectionIntegrationTest.kt).

# Prerequisites
- Java 21+

# Running Tests
```shell
./gradlew clean check --info
```
