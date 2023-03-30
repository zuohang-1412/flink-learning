package org.window

import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.sql.{Connection, DriverManager, PreparedStatement}

object timeWindowKeyStreamTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 因为报错：Record has Long.MIN_VALUE timestamp (= no timestamp marker).
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    env.setParallelism(1)
    // word count
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val keyStream: KeyedStream[(String, Int), String] = stream.flatMap(_.split(" ")).map((_, 1)).keyBy(_._1)
    keyStream
      .timeWindow(Time.seconds(10))
      .reduce(new ReduceFunction[(String, Int)] {
        override def reduce(value1: (String, Int), value2: (String, Int)): (String, Int) = {
          (value1._1, value2._2 + value1._2)
        }
      }, new WindowFunction[(String, Int), (String, Int), String, TimeWindow] {
        override def apply(key: String, window: TimeWindow, input: Iterable[(String, Int)], out: Collector[(String, Int)]): Unit = {
          // 创建链接
          val conn: Connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/data", "root", "zuohang123")
          val statement: PreparedStatement = conn.prepareStatement("insert into wc_time_window values (?,?,?,?)")
          statement.setLong(1, window.getStart)
          statement.setLong(2, window.getEnd)
          statement.setString(3, input.head._1)
          statement.setInt(4, input.head._2)
          statement.execute()
          statement.close()
          conn.close()
        }
      }).print()
    env.execute()
  }
}
