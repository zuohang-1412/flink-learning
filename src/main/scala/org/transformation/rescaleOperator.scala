package org.transformation

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

object rescaleOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[Long] = env.fromSequence(1, 10).setParallelism(2)
    // 10 个数 分在两个文件中
    stream.writeAsText("./data/rescale1").setParallelism(2)
    // stream1 中的一个文件 分在 stream2 两个文件中
    stream.rescale.writeAsText("./data/rescale2").setParallelism(4)
    env.execute()
  }
}
