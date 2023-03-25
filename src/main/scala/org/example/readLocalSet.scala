package org.example

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._

object readLocalSet {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val data: DataStream[Int] = env.fromCollection(List(1, 2, 3))
    data.print()
    env.execute()
  }
}
