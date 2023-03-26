package org.source

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}

object readKafka {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val kafkaSource: KafkaSource[String] = KafkaSource.builder().setBootstrapServers("localhost:9092")
      .setTopics("carFlow")
      .setGroupId("my-group")
      // timestamp(1657256176000L)(从时间戳大于等于指定时间戳（毫秒）的数据开始消费)   latest(从最末尾位点开始消费)
      .setStartingOffsets(OffsetsInitializer.earliest()) // 默认值 earliest() 从最早位点开始消费
      .setValueOnlyDeserializer(new SimpleStringSchema())
      .build()
    // 关联Source
    //    val props = new Properties()
    //    props.setProperty("bootstrap.servers", "localhost:9092")
    //    props.setProperty("group.id", "consumer-group")
    //    env.addSource(new FlinkKafkaConsumer[String]("test", new SimpleStringSchema(), props))
    // WatermarkStrategy.noWatermarks() 水印策略，不使用水印
    val data = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")
    data.print()

    env.execute()
  }
}
