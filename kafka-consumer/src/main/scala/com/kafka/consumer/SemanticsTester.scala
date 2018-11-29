package com.kafka.consumer

import scala.collection.mutable

import com.kafka.consumer.KafkaConsumer.log

object SemanticsTester {
  val maxQueueSize = 10000
  private val arrivalQueue = mutable.TreeSet.empty[Int]

  resetArrivalQueue(None)

  def resetArrivalQueue(watermark: Option[Int]): Unit = {
    arrivalQueue.clear()
    watermark match {
      case None =>
        arrivalQueue.add(-1)
      case w: Some[Int] =>
        arrivalQueue.add(w.get)
    }
  }

  def getWaterMark: Int = {
    arrivalQueue.head
  }

  def checkExactlyOnce(offset: Int): Unit = {
    if (offset > getWaterMark) {
      if (!arrivalQueue.add(offset)) {
        log.error(s"Arrived the same data with offset $offset, exactly-once semantics broken")
        throw new Exception
      }
      if (arrivalQueue.size > maxQueueSize) {
        log.error(s"Gaps in the arrival queue which reached it's max size ($maxQueueSize), maybe missing data?!")
        throw new Exception
      }
      var consecutive = true
      val iterator = arrivalQueue.iterator
      var data = iterator.next()
      while (iterator.hasNext) {
        val next = iterator.next()
        if (next - data != 1) {
          consecutive = false
        }
        data = next
      }
      if (consecutive) {
        resetArrivalQueue(Some(arrivalQueue.max))
      }
    } else {
      log.error(s"Arrived the same data with offset $offset, exactly-once semantics broken")
      throw new Exception
    }
  }
}
