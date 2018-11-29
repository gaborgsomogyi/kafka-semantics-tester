package com.kafka.producer

import java.util.Properties

import scala.util.Random
import scala.util.control.NonFatal

import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.log4j.LogManager

object KafkaProducer {

  @transient lazy val log = LogManager.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    if (args.length != 4) {
      log.error("Usage: KafkaProducer [bootstrap] [topic] [messages_per_burst] [sleep_in_ms]")
      log.error("Example: KafkaProducer localhost:9092 src-topic 10 1000")
      System.exit(1)
    }

    val bootstrapServer = args(0)
    val topicName = args(1)
    val messagesPerBurst = args(2).toInt
    val sleepInMs = args(3).toInt

    log.info("Creating config properties...")
    val props = new Properties
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
    props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "producer-static-transactional-id")
    log.info("OK")

    log.info("Creating kafka producer...")
    val producer = new KafkaProducer[String, String](props)
    producer.initTransactions()
    log.info("OK")

    try {
      var startOffset = 0L
      while (true) {
        val rnd = new Random().nextInt(101)
        if (rnd < 90) {
          sendData(producer, topicName, startOffset, messagesPerBurst, false)
        } else {
          sendData(producer, topicName, startOffset, messagesPerBurst, true)
          sendData(producer, topicName, startOffset, messagesPerBurst, false)
        }
        Thread.sleep(sleepInMs)
        startOffset += messagesPerBurst
      }
    } catch {
      case NonFatal(e) =>
        log.error(e)
        e.printStackTrace()
        producer.abortTransaction()
    } finally {
      log.info("Closing kafka producer...")
      producer.close()
      log.info("OK")
    }
  }

  private def sendData(
      producer: KafkaProducer[String, String],
      topicName: String,
      startOffset: Long,
      messagesPerBurst: Int,
      fail: Boolean): Unit = {
    producer.beginTransaction()
    for (i <- 0 until messagesPerBurst) {
      val offset = startOffset + i
      val data = "streamtest-" + offset
      val record = new ProducerRecord[String, String](topicName, null, data)
      producer.send(record, onSendCallback(offset))
      log.info("Record: " + record)
    }
    if (!fail) {
      log.info("Commit")
      producer.commitTransaction()
    } else {
      log.info("Abort")
      producer.abortTransaction()
    }
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
