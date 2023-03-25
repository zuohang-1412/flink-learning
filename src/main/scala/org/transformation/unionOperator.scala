package org.transformation

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._

object unionOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream1: DataStream[(String, Int)] = env.fromCollection(List(("a", 1), ("b", 1)))
    val stream2: DataStream[(String, Int)] = env.fromCollection(List(("a", 1), ("b", 1)))
    // 只有类型相同的 DataStream 才可以union
    val unionStream: DataStream[(String, Int)] = stream1.union(stream2)
    unionStream.print()

    env.execute()
  }
}
