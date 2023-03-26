package org.sink

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.connector.kafka.sink.KafkaSink
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic

import java.util.Properties
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaProducer, KafkaSerializationSchema}
import org.apache.kafka.clients.producer.ProducerRecord

import java.lang

object kafkaSinkTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val props = new Properties()
    props.setProperty("bootstrap.servers", "localhost:9092")
    stream.flatMap(_.split(" "))
      .map((_,1))
      .keyBy(0)
      .sum(1)
      .addSink(new FlinkKafkaProducer[(String, Int)](
        "flink",
        new KafkaSerializationSchema[(String, Int)] {
          override def serialize(element: (String, Int), timestamp: lang.Long): ProducerRecord[Array[Byte], Array[Byte]] = {
            new ProducerRecord[Array[Byte], Array[Byte]]("wc",element._1.getBytes(),element._2.toString.getBytes())
          }
        },
        props,
        Semantic.EXACTLY_ONCE
      ))
    env.execute()
  }
}
