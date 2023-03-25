package org.transformation

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.api.common.functions.Partitioner

object partitionOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[(Long, Int)] = env.fromSequence(1, 10).map((_, 1)).setParallelism(2)
    stream.writeAsText("./data/partition1").setParallelism(2)
    // 根据哪个字段来分区
    stream.partitionCustom(new customPartitioner(), 0).writeAsText("./data/partition2").setParallelism(4)
    env.execute()
  }

  class customPartitioner extends Partitioner[Long] {
    override def partition(k: Long, i: Int): Int = {
      k.toInt % i
    }
  }
}
