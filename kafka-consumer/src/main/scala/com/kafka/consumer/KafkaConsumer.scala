package com.kafka.consumer

import java.time.Duration
import java.util
import java.util.{Locale, Properties}

import scala.collection.JavaConversions._

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.requests.IsolationLevel
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.LogManager

object KafkaConsumer {

  @transient lazy val log = LogManager.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      log.error("Usage: KafkaConsumer [bootstrap] [topic]")
      log.error("Example: KafkaConsumer localhost:9092 dst-topic")
      System.exit(1)
    }

    val bootstrapServer = args(0)
    val topicName = args(1)

    log.info("Creating config properties...")
    val props = new Properties
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-static-group-id")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.toString.toLowerCase(Locale.ROOT))
    log.info("OK")

    log.info("Creating kafka consumer...")
    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(util.Arrays.asList(topicName))
    log.info("OK")

    try {
      while (true) {
        val records = consumer.poll(Duration.ofMillis(1000))
        for (record <- records) {
          log.info("Record: " + record)
          try {
            val offset = record.value().replace("streamtest-", "").toInt
            SemanticsTester.checkExactlyOnce(offset)
          } catch {
            case e: NumberFormatException =>
              log.error("Invalid data in stream")
              throw e
          }
        }
        consumer.commitSync()
        Thread.sleep(100)
      }
    } finally {
      log.info("Closing kafka consumer...")
      consumer.close()
      log.info("OK")
    }
  }
}
