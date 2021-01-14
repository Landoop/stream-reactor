/*
 * Copyright 2017 Datamountaineer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lenses.streamreactor.connect.azure.servicebus

import java.util
import java.util.Date
import com.azure.core.util.{BinaryData, IterableStream}
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient
import com.azure.messaging.servicebus.administration.models.{QueueRuntimeProperties, SubscriptionRuntimeProperties}
import com.azure.messaging.servicebus.{ServiceBusReceivedMessage, ServiceBusReceiverClient}
import com.datamountaineer.streamreactor.connect.schemas.ConverterUtil
import com.fasterxml.jackson.databind.JsonNode
import io.lenses.streamreactor.connect.azure.servicebus.config.AzureServiceBusConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.record.TimestampType
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.sink.SinkRecord
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.JavaConverters._
import scala.collection.mutable

trait TestBase extends AnyWordSpec with Matchers with MockitoSugar with ConverterUtil {

  val TOPIC = "servicebus-topic"
  val QUEUE = "servicebus-queue"
  val CONNECTOR_NAME = "servicebusconnector"
  val PARTITION: Int = 12
  val TOPIC_PARTITION: TopicPartition =
    new TopicPartition(TOPIC, PARTITION)
  val ASSIGNMENT: util.Set[TopicPartition] =
    new util.HashSet[TopicPartition]
  //Set topic assignments
  ASSIGNMENT.add(TOPIC_PARTITION)

  val DATE = new Date()

  val mockManagementClient: ServiceBusAdministrationClient = mock[ServiceBusAdministrationClient]
  when(mockManagementClient.getTopicExists(TOPIC)).thenReturn(true)
  when(mockManagementClient.getQueueExists(QUEUE)).thenReturn(true)

  val mockQueueResponse: QueueRuntimeProperties = mock[QueueRuntimeProperties]
  val mockSubscriptionResponse: SubscriptionRuntimeProperties = mock[SubscriptionRuntimeProperties]
  when(mockQueueResponse.getTotalMessageCount).thenReturn(1)
  when(mockSubscriptionResponse.getTotalMessageCount).thenReturn(1)

  when(mockManagementClient.getSubscriptionRuntimeProperties(TOPIC, CONNECTOR_NAME)).thenReturn(mockSubscriptionResponse)
  when(mockManagementClient.getQueueRuntimeProperties(QUEUE)).thenReturn(mockQueueResponse)


  val mockClient: ServiceBusReceiverClient = mock[ServiceBusReceiverClient]
  val serviceBusMessage: ServiceBusReceivedMessage = mock[ServiceBusReceivedMessage]
  val connectRecord: SinkRecord = getTestRecords(1).head
  val jsonRecord: JsonNode = convertValueToJson(connectRecord)
  val binaryData: BinaryData = BinaryData.fromBytes(jsonRecord.toString.getBytes)

  when(serviceBusMessage.getPartitionKey).thenReturn("my-partition-value")
  when(serviceBusMessage.getMessageId).thenReturn("my-message-id")
  when(serviceBusMessage.getBody).thenReturn(binaryData)
  when(serviceBusMessage.getLockToken).thenReturn("mylocktoken")
  when(serviceBusMessage.getDeliveryCount).thenReturn(1)
  when(serviceBusMessage.getSequenceNumber).thenReturn(2)
  when(serviceBusMessage.getCorrelationId).thenReturn("mycorrelationid")
  val is: IterableStream[ServiceBusReceivedMessage] = IterableStream.of(List(serviceBusMessage).toIterable.asJava)

  when(mockClient.peekMessages(500)).thenReturn(is)

  //get the assignment of topic partitions for the sinkTask
  def getAssignment: util.Set[TopicPartition] = {
    ASSIGNMENT
  }

  def getProps(kcql: String): util.Map[String, String] = {
    Map(
      AzureServiceBusConfig.AZURE_SAP_NAME -> "myname",
      AzureServiceBusConfig.AZURE_SAP_KEY -> "mykey",
      AzureServiceBusConfig.AZURE_SB_NAMESPACE -> "mynamespace",
      AzureServiceBusConfig.KCQL -> kcql,
      AzureServiceBusConfig.SET_HEADERS -> "true"
    ).asJava
  }

  //build a test record schema
  def createSchema: Schema = {

    val lDate = org.apache.kafka.connect.data.Date.builder().build()
    val lTime = org.apache.kafka.connect.data.Time.builder().build()
    val lTimestamp = org.apache.kafka.connect.data.Timestamp.builder().build()

    SchemaBuilder.struct
      .name("record")
      .version(1)
      .field("id", Schema.STRING_SCHEMA)
      .field("int8_field", Schema.INT8_SCHEMA)
      .field("int8_field_optional", Schema.OPTIONAL_INT8_SCHEMA)
      .field("int32_field", Schema.INT32_SCHEMA)
      .field("int32_field_optional",
             SchemaBuilder.int32().defaultValue(1).optional())
      .field("int64_field", Schema.INT64_SCHEMA)
      .field("int64_field_optional",
             SchemaBuilder.int64().defaultValue(1L).optional())
      .field("string_field", Schema.STRING_SCHEMA)
      .field("string_field_optional",
             SchemaBuilder.string().defaultValue("boo").optional())
      .field("boolean_field", Schema.BOOLEAN_SCHEMA)
      .field("boolean_field_optional",
             SchemaBuilder.bool().defaultValue(false).optional())
      .field("byte_field", Schema.BYTES_SCHEMA)
      .field("byte_field_optional",
             SchemaBuilder.bytes().defaultValue("bar".getBytes).optional())
      .field("float32_field", Schema.FLOAT32_SCHEMA)
      .field("float32_field_optional",
             SchemaBuilder.float32().defaultValue(0.1f).optional())
      .field("float64_field", Schema.FLOAT64_SCHEMA)
      .field("float64_field_optional", SchemaBuilder.float64().defaultValue(0.1.toDouble).optional())
      .field("logical_date", lDate)
      .field("logical_time", lTime)
      .field("logical_timestamp", lTimestamp)
      .build
  }

  //build a test record
  def createRecord(schema: Schema, id: String): Struct = {
    new Struct(schema)
      .put("id", id)
      .put("int8_field", 2.toByte)
      .put("int8_field_optional", 3.toByte)
      .put("int32_field", 12)
      .put("int64_field", 12L)
      .put("string_field", "foo")
      .put("boolean_field", true)
      .put("byte_field", "bytes".getBytes)
      .put("float32_field", 1.1234567.toFloat)
      .put("float64_field", 1.123456789101112)
      .put("logical_date", DATE)
      .put("logical_time", DATE)
      .put("logical_timestamp", DATE)
  }

  def createSinkRecord(record: Struct,
                       topic: String,
                       offset: Long): SinkRecord = {
    new SinkRecord(topic,
                   1,
                   Schema.STRING_SCHEMA,
                   "key",
                   record.schema(),
                   record,
                   offset)
  }

  //generate some test records
  def getTestRecords(nbr: Int = 1): Seq[SinkRecord] = {
    val schema = createSchema
    val assignment: mutable.Set[TopicPartition] = getAssignment.asScala

    assignment
      .flatMap(a => {
        (1 to nbr).map(i => {
          val record: Struct =
            createRecord(schema, a.topic() + "-" + a.partition() + "-" + i)
          new SinkRecord(a.topic(),
                         a.partition(),
                         Schema.STRING_SCHEMA,
                         "key",
                         schema,
                         record,
                         i,
                         System.currentTimeMillis(),
                         TimestampType.CREATE_TIME)
        })
      })
      .toSeq
  }

}
