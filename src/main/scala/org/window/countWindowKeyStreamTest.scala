package org.window

import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.streaming.api.scala.{DataStream, KeyedStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._

object countWindowKeyStreamTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // A A A A a a A A A A a A A
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val keyStream: KeyedStream[(String, Int), String] = stream.flatMap(_.split(" ")).map((_, 1)).keyBy(_._1)
    // 10 的意思是 相同的key 10次才会窗口移动
    keyStream.countWindow(10).reduce(new ReduceFunction[(String, Int)] {
      override def reduce(value1: (String, Int), value2: (String, Int)): (String, Int) = {
        (value1._1, value1._2 + value2._2)
      }
    }).print()
    env.execute()
  }
}
