package org.transformation

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.api.java.functions.KeySelector

object keybyOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val res = stream.flatMap(_.split(" ")).map((_, 1)).keyBy(new KeySelector[(String, Int), String] {
      override def getKey(in: (String, Int)): String = {
        in._1
      }
    }).sum(1)
    res.print()


    env.execute()
  }
}
