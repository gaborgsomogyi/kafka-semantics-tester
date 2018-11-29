package com.kafka.standalone.processor

import java.time.Duration
import java.util
import java.util.{Locale, Properties}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.Random

import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecords, KafkaConsumer, OffsetAndMetadata}
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.requests.IsolationLevel
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.apache.log4j.LogManager

trait SendStrategy[K, V] {
  @transient lazy val log = LogManager.getLogger(getClass)

  def init(): Unit

  def send(records: ConsumerRecords[K, V]): Unit

  def injectFault(): Unit = {
    val rnd = new Random().nextInt(101)
    if (rnd >= 90) {
      throw new Exception("Intentional exception to break semantics")
    }
  }

  def commitConsumerSync(): Unit
}

class AtLeastOnceSendStrategy[K, V](consumer: KafkaConsumer[K, V], producer: KafkaProducer[K, V], dstTopicName: String)
    extends SendStrategy[K, V] {

  def init(): Unit = {
    // Here don't need any kind of transactions
  }

  def send(records: ConsumerRecords[K, V]): Unit = {
    for (record <- records) {
      log.info("Record: " + record)
      val dstRecord = new ProducerRecord[K, V](dstTopicName, record.key(), record.value())
      producer.send(dstRecord, onSendCallback(record.offset()))
    }
  }

  def commitConsumerSync(): Unit = {
    consumer.commitSync()
  }

  def onSendCallback(messageNumber: Long): Callback = new Callback() {
    override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
      if (exception == null) {
        log.info(s"Message $messageNumber sent to topic ${metadata.topic()} in partition ${metadata.partition()} at offset ${metadata.offset()}")
      } else {
        log.error(s"Error sending message $messageNumber")
        log.error(exception)
      }
    }
  }
}

class ExactlyOnceSendStrategy[K, V](consumer: KafkaConsumer[K, V], producer: KafkaProducer[K, V], dstTopicName: String, consumerGroupId: String)
    extends AtLeastOnceSendStrategy(consumer, producer, dstTopicName) {

  override def init(): Unit = {
    producer.initTransactions()
  }

  private def send(records: ConsumerRecords[K, V], offsetsToCommit: mutable.Map[TopicPartition, OffsetAndMetadata], fail: Boolean): Unit = {
    producer.beginTransaction()
    super.send(records)
    producer.sendOffsetsToTransaction(offsetsToCommit, consumerGroupId)
    if (!fail) {
      log.info("Commit")
      producer.commitTransaction()
    } else {
      log.info("Abort")
      producer.abortTransaction()
    }
  }

  override def send(records: ConsumerRecords[K, V]): Unit = {
    val offsetsToCommit = mutable.Map.empty[TopicPartition, OffsetAndMetadata]
    for (partition <- records.partitions) {
      val partitionedRecords = records.records(partition)
      val offset = partitionedRecords.get(partitionedRecords.size() - 1).offset()
      offsetsToCommit.put(partition, new OffsetAndMetadata(offset + 1))
    }
    val rnd = new Random().nextInt(101)
    if (rnd < 90) {
      send(records, offsetsToCommit, false)
    } else {
      send(records, offsetsToCommit, true)
      send(records, offsetsToCommit, false)
    }
  }

  override def commitConsumerSync(): Unit = {
    // Here commit happens inside send in one transaction
  }
}

object KafkaProcessor {

  @transient lazy val log = LogManager.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    if (args.length != 4) {
      log.error("Usage: KafkaProcessor [bootstrap] [src-topic] [dst-topic] exactly-once")
      log.error("Example: KafkaProcessor localhost:9092 src-topic dst-topic false")
      System.exit(1)
    }

    val bootstrapServer = args(0)
    val srcTopicName = args(1)
    val dstTopicName = args(2)
    val exactlyOnce = args(3).toBoolean

    val consumerGroupId = "processor-static-group-id"

    log.info("Creating config properties...")
    val consumerProps = new Properties
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId)
    consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    val isolationLevel = if (!exactlyOnce) {
      IsolationLevel.READ_UNCOMMITTED
    } else {
      IsolationLevel.READ_COMMITTED
    }
    consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel.toString.toLowerCase(Locale.ROOT))
    log.info("OK")

    log.info("Creating kafka consumer...")
    val consumer = new KafkaConsumer[String, String](consumerProps)
    consumer.subscribe(util.Arrays.asList(srcTopicName))
    log.info("OK")

    log.info("Creating config properties...")
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    if (exactlyOnce) {
      producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
      producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "processor-static-transactional-id")
    }
    log.info("OK")

    log.info("Creating kafka producer...")
    val producer = new KafkaProducer[String, String](producerProps)
    log.info("OK")

    log.info("Creating kafka producer send strategy...")
    val producerSendStrategy = if (!exactlyOnce) {
      new AtLeastOnceSendStrategy(consumer, producer, dstTopicName)
    } else {
      new ExactlyOnceSendStrategy(consumer, producer, dstTopicName, consumerGroupId)
    }
    producerSendStrategy.init()
    log.info("OK")

    try {
      while (true) {
        val records = consumer.poll(Duration.ofMillis(1000))
        if (!records.isEmpty) {
          producerSendStrategy.send(records)
          producerSendStrategy.injectFault()
          producerSendStrategy.commitConsumerSync()
        }
        Thread.sleep(100)
      }
    } finally {
      log.info("Closing kafka consumer...")
      consumer.close()
      log.info("OK")
    }
  }
}
