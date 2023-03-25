package org.transformation

import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, DataStream}

object rebalanceOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 从一个分区
    val stream: DataStream[Long] = env.fromSequence(1, 100).setParallelism(3)
    println(stream.parallelism)
    // rebalance 平均分发到多个分区中
    stream.rebalance.print()
    env.execute()
  }
}
