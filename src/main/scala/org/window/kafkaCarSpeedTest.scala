package org.window

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object kafkaCarSpeedTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val kafkaSource: KafkaSource[String] = KafkaSource.builder().setBootstrapServers("localhost:9092")
      .setTopics("carFlow")
      .setGroupId("speed-sorted")
      // timestamp(1657256176000L)(从时间戳大于等于指定时间戳（毫秒）的数据开始消费)   latest(从最末尾位点开始消费)
      .setStartingOffsets(OffsetsInitializer.earliest()) // 默认值 earliest() 从最早位点开始消费
      .setValueOnlyDeserializer(new SimpleStringSchema())
      .build()
    // 卡口号、车牌号、事件时间、车速
    val data = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")
    data.map(row => {
      val arrs: Array[String] = row.split("\t")
      (arrs(1), arrs(3).toLong)
    }).filter(_._2 != "00000000")
      .timeWindowAll(Time.minutes(30),Time.seconds(5))
      .process(new ProcessAllWindowFunction[(String, Long), (String, Long),TimeWindow] {
        override def process(context: Context, elements: Iterable[(String, Long)], out: Collector[(String, Long)]): Unit = {
          val tuples: List[(String, Long)] = elements.toList.sortBy(_._2)
          val lowCar: String = tuples.head._1
          val fastCar: String = tuples.last._1
          println(s"lowCar: $lowCar, fastCar: $fastCar")
          for (tuple <-tuples) {
            out.collect(tuple)
          }
        }
      }).print()
    env.execute()
  }
}
