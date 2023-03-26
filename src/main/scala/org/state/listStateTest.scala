package org.state

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.kafka.common.serialization.StringSerializer

import java.text.SimpleDateFormat
import java.util.Properties
import org.apache.flink.streaming.api.scala._

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object listStateTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val kafkaSource: KafkaSource[String] = KafkaSource.builder().setBootstrapServers("localhost:9092")
      .setTopics("carFlow")
      .setGroupId("list_state")
      .setStartingOffsets(OffsetsInitializer.earliest()) // 默认值 earliest() 从最早位点开始消费
      .setValueOnlyDeserializer(new SimpleStringSchema())
      .build()
    //卡口号、车牌号、事件时间、车速
    val stream = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss")

    stream.map(row => {
      val arrs: Array[String] = row.split("\t")
      val times: Long = format.parse(arrs(2)).getTime
      (arrs(0), arrs(1), times, arrs(3).toLong)
    }).filter(_._2!="00000000")
      .keyBy(_._2)
      .map(new RichMapFunction[(String, String, Long, Long), (String, String)] {

        private var carInfos: ListState[(String, Long)] = _

        override def open(parameters: Configuration): Unit = {
          val desc = new ListStateDescriptor[(String, Long)]("list", createTypeInformation[(String, Long)])
          carInfos = getRuntimeContext.getListState(desc)
        }

        override def map(elem: (String, String, Long, Long)): (String, String) = {
          carInfos.add((elem._1, elem._3))

          val seq = carInfos.get().asScala.seq
          val sortList = seq.toList.sortBy(_._2)

          val builder = new StringBuilder
          for (elem <- sortList) {
            builder.append(elem._1 + "\t")
          }
          (elem._2, builder.toString())
        }
      }).print()
    env.execute()
  }
}
