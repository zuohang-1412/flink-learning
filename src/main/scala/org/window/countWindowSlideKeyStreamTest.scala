package org.window

import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.streaming.api.scala._

object countWindowSlideKeyStreamTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // A A A A a a A A A A a A A
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val keyStream: KeyedStream[(String, Int), String] = stream.flatMap(_.split(" ")).map((_, 1)).keyBy(_._1)
    // 窗口中每新增两个相同元素 都会滑动一次窗口（1、触发窗口计算，2、开启新窗口），
    keyStream.countWindow(10, 2).reduce(new ReduceFunction[(String, Int)] {
      override def reduce(value1: (String, Int), value2: (String, Int)): (String, Int) = {
        (value1._1, value1._2 + value2._2)
      }
    }).print()
    env.execute()
  }
}
