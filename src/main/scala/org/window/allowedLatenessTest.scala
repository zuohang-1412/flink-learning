package org.window

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala.{OutputTag, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

// 延迟数据处理， 不想丢弃数据的话
object allowedLatenessTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)
    /*
    10000 A
    11000 B
    12000 C
    13000 D
    14000 E
    15000 F
    17000 G
    13000 H
    20000 I
    13000 J
    H 能计算进window 但是 J无法进入，只能进入侧输出流再做处置
     */
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val lateTag: OutputTag[(Long, String)] = new OutputTag[(Long, String)]("lasted")
    val res: DataStream[(Long, String)] = stream.map(row => {
      val arrs: Array[String] = row.split(" ")
      (arrs(0).toLong, arrs(1))
    }).assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[(Long, String)](Time.seconds(2)) {
      override def extractTimestamp(element: (Long, String)): Long = element._1
    }).timeWindowAll(Time.seconds(5))
      .allowedLateness(Time.seconds(3))
      .sideOutputLateData(lateTag)
      .process(new ProcessAllWindowFunction[(Long, String), (Long, String), TimeWindow] {
        override def process(context: Context, elements: Iterable[(Long, String)], out: Collector[(Long, String)]): Unit = {
          println(context.window.getStart + "---" + context.window.getEnd)
          for (elem <- elements) {
            out.collect(elem)
          }
        }
      })
    res.print("main")
    res.getSideOutput(lateTag).print("late")
    env.execute()
  }
}
