package org.example

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

object streamWordCount {
  def main(args: Array[String]): Unit = {
    // 创建环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    // 读取数据
    val lines: DataStream[String] = env.readTextFile("./data/wordCount.txt")

    import org.apache.flink.streaming.api.scala._
    val res: DataStream[(String, Int)] = lines.flatMap(_.split(" ")).map((_, 1)).keyBy(0).sum(1)
    res.print()

    // 触发执行
    env.execute()
  }
}
