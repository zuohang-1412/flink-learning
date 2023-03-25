package org.transformation

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

object shuffleOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 从一个分区
    val stream: DataStream[Long] = env.fromSequence(1, 10).setParallelism(1)
    println(stream.parallelism)
    // shuffle 随机分发到多个分区中
    stream.shuffle.print()
    env.execute()
  }
}
