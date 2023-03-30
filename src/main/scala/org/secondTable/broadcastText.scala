package org.secondTable

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.datastream.BroadcastStream
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.apache.flink.util.Collector

object broadcastText {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 获取kafka 数据
    val kafkaSource: KafkaSource[String] = KafkaSource.builder().setBootstrapServers("localhost:9092")
      .setTopics("cityInfo")
      .setGroupId("my-group")
      // timestamp(1657256176000L)(从时间戳大于等于指定时间戳（毫秒）的数据开始消费)   latest(从最末尾位点开始消费)
      .setStartingOffsets(OffsetsInitializer.earliest()) // 默认值 earliest() 从最早位点开始消费
      .setValueOnlyDeserializer(new SimpleStringSchema())
      .build()

    // 维度数据
    val data = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source")
    // 业务流
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    //定义map state描述器
    val descriptor = new MapStateDescriptor[String, String]("dynamicConfig",
      BasicTypeInfo.STRING_TYPE_INFO,
      BasicTypeInfo.STRING_TYPE_INFO)
    //设置广播流的数据描述信息
    val broadcastStream: BroadcastStream[String] = data.broadcast(descriptor)
    stream.connect(broadcastStream)
      .process(new BroadcastProcessFunction[String, String, String] {
        //每来一个新的元素都会调用一下这个方法
        override def processElement(value: String, ctx: BroadcastProcessFunction[String, String, String]#ReadOnlyContext, out: Collector[String]): Unit = {
          val broadcast = ctx.getBroadcastState(descriptor)
          val city = broadcast.get(value)
          if (city == null) {
            out.collect("not found city")
          } else {
            out.collect(city)
          }
        }

        //kafka中配置流信息，写入到广播流中
        override def processBroadcastElement(value: String, ctx: BroadcastProcessFunction[String, String, String]#Context, out: Collector[String]): Unit = {
          val broadcast = ctx.getBroadcastState(descriptor)
          //kafka中的数据
          val elems = value.split(" ")
          broadcast.put(elems(0), elems(1))
        }
      }).print()
    env.execute()
  }
}
