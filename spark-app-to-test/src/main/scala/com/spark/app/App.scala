/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spark.app

import java.util.UUID

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.{OutputMode, Trigger}

object App {
  def main(args: Array[String]) {
    if (args.length != 4) {
      System.err.println(s"""
                            |Usage: App <bootstrap-servers> <src-topic> <src-topic>]
                            |  <bootstrap-servers> The Kafka "bootstrap.servers" configuration.
                            |  A comma-separated list of host:port.
                            |  <src-topic> Source topic.
                            |  <dst-topic> Destination topic.
                            |  <checkpoint> Checkpoint location.
                            |
      """.stripMargin)
      System.exit(1)
    }

    val Array(bootstrapServers, srcTopic, dstTopic, checkpoint) = args

    val kafkaSourceParams = Map[String, String](
      "kafka.bootstrap.servers" -> bootstrapServers,
      "subscribe" -> srcTopic,
      "startingoffsets" -> "earliest"
    )

    val spark = SparkSession
      .builder
      .appName("App")
      .getOrCreate()

    import spark.implicits._
    val events = spark
      .readStream
      .format("kafka")
      .options(kafkaSourceParams)
      .load()
      .selectExpr("CAST(value AS STRING)")
      .as[String]

    val kafkaDestinationParams = Map[String, String](
      "kafka.bootstrap.servers" -> bootstrapServers,
      "topic" -> dstTopic
    )

    val query = events
      .writeStream
      .trigger(Trigger.Continuous("1 second"))
      .outputMode(OutputMode.Append)
      .format("kafka")
      .options(kafkaDestinationParams)
      .option("checkpointLocation", checkpoint)
      .start()

    query.awaitTermination()
  }
}
