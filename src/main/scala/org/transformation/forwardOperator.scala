package org.transformation

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

object forwardOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[Long] = env.fromSequence(1, 10).setParallelism(2)
    stream.writeAsText("./data/stream5").setParallelism(2)
    // forward 前后分区必须一致 setParallelism(2) != setParallelism(4)
    stream.forward.writeAsText("./data/stream6").setParallelism(4)
    env.execute()
  }
}
