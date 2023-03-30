package org.window

import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object EventTimeTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 指定时间语意
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    // 数据格式：时间戳 单词 （eg： 10000 AAA BBB CC）
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    // 指定 代表 EventTime 字段所在位置
    val ETStream: DataStream[String] = stream.assignAscendingTimestamps(data => {
      val arrs: Array[String] = data.split(" ")
      arrs(0).toLong
    })
    // 去除 第一列 之后的数据进行 wordCount
    ETStream.flatMap(_.split(" ").tail)
      .map((_,1))
      .keyBy(_._1)
      // 窗口左闭右开[）
      .timeWindow(Time.seconds(10))
      .reduce(new ReduceFunction[(String, Int)] {
        override def reduce(value1: (String, Int), value2: (String, Int)): (String, Int) = {
          (value1._1, value1._2+value2._2)
        }
      },
        new WindowFunction[(String, Int), (String, Int), String, TimeWindow] {
          override def apply(key: String, window: TimeWindow, input: Iterable[(String, Int)], out: Collector[(String, Int)]): Unit = {
            val start: Long = window.getStart
            val end: Long = window.getEnd
            println(s"window start:$start, end:$end")
            for (i <-input) {
              out.collect(i)
            }
          }
        }).print()
    env.execute()
  }
}
