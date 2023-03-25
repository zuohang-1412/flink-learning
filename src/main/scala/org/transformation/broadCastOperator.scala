package org.transformation

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

object broadCastOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[Long] = env.fromSequence(1, 10).setParallelism(2)
    stream.writeAsText("./data/broad1").setParallelism(2)
    // 4个文件中都是 1-10
    stream.broadcast.writeAsText("./data/broad2").setParallelism(4)
    env.execute()
  }
}
