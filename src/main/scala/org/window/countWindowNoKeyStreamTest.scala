package org.window

import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.streaming.api.scala._

object countWindowNoKeyStreamTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val mapStream: DataStream[(String, Int)] = stream.flatMap(_.split(" ")).map((_, 1))
    // 十个单词计算一次
    mapStream.countWindowAll(5)
      .reduce(new ReduceFunction[(String, Int)] {
      override def reduce(value1: (String, Int), value2: (String, Int)): (String, Int) = {
        (value1._1+";"+value2._1, value1._2+value2._2)
      }
    }).print()
    env.execute()
  }
}
