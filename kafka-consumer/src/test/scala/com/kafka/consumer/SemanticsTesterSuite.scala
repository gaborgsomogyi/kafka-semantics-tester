package com.kafka.consumer

import org.scalatest.FunSuite

class SemanticsTesterSuite extends FunSuite {
  test("checkExactlyOnce should increase watermark if offset is watermark + 1") {
    SemanticsTester.resetArrivalQueue(None)
    SemanticsTester.checkExactlyOnce(0)
    assert(SemanticsTester.getWaterMark === 0)
  }

  test("checkExactlyOnce should throw exception if offset is watermark + 2 and sent more times") {
    SemanticsTester.resetArrivalQueue(None)
    SemanticsTester.checkExactlyOnce(1)
    assert(SemanticsTester.getWaterMark === -1)
    assertThrows[Exception] {
      SemanticsTester.checkExactlyOnce(1)
    }
    assert(SemanticsTester.getWaterMark === -1)
  }

  test("checkExactlyOnce should increase watermark by 2 if offsets are watermark + 2, watermark + 1") {
    SemanticsTester.resetArrivalQueue(None)
    SemanticsTester.checkExactlyOnce(1)
    SemanticsTester.checkExactlyOnce(0)
    assert(SemanticsTester.getWaterMark === 1)
  }

  test("checkExactlyOnce should throw exception if offset is under/equals to watermark") {
    SemanticsTester.resetArrivalQueue(Some(1))
    assertThrows[Exception] {
      SemanticsTester.checkExactlyOnce(1)
    }
    assertThrows[Exception] {
      SemanticsTester.checkExactlyOnce(0)
    }
  }

  test("checkExactlyOnce should throw exception if gaps are in the arrival data for long time") {
    SemanticsTester.resetArrivalQueue(None)
    for (i <- 1 until SemanticsTester.maxQueueSize) {
      SemanticsTester.checkExactlyOnce(i)
    }
    assertThrows[Exception] {
      SemanticsTester.checkExactlyOnce(SemanticsTester.maxQueueSize + 1)
    }
  }
}
