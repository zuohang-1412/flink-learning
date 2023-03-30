package org.window

import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.{AssignerWithPeriodicWatermarks, AssignerWithPunctuatedWatermarks}
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

object waterMarkTest3 {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    // 100ms 进行一次 waterMark 操作
    env.getConfig.setAutoWatermarkInterval(100)
    // 设置水位线时间
    val delay: Long = 3000L
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val WMStream: DataStream[String] = stream.assignTimestampsAndWatermarks(
      new AssignerWithPunctuatedWatermarks[String] {
        var maxTime: Long = _
        // 根据数据条件生成 水位线
        override def checkAndGetNextWatermark(lastElement: String, extractedTimestamp: Long): Watermark = {
          val str: String = lastElement.split(" ")(1)
          if ("001".equals(str)) {
            new Watermark(maxTime - 3000)
          }else {
            null
          }
        }

        override def extractTimestamp(element: String, recordTimestamp: Long): Long = {
          val time: String = element.split(" ")(0)
          maxTime = maxTime.max(time.toLong)
          time.toLong
        }
      }
    )
    WMStream
      .flatMap(x => x.split(" ").tail)
      .map((_, 1))
      .keyBy(_._1)
      //滚动窗口
      .timeWindow(Time.seconds(10))
      .reduce(new ReduceFunction[(String, Int)] {
        override def reduce(value1: (String, Int), value2: (String, Int)): (String, Int) = {
          (value1._1, value1._2 + value2._2)
        }
      }, new WindowFunction[(String, Int), (String, Int), String, TimeWindow] {
        override def apply(key: String, window: TimeWindow, input: Iterable[(String, Int)], out: Collector[(String, Int)]): Unit = {
          val start = window.getStart
          val end = window.getEnd
          println("window start:" + start + "--- end:" + end)
          for (elem <- input) {
            out.collect(elem)
          }
        }
      })
      .print()
    env.execute()
  }
}
