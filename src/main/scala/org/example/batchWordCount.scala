package org.example

import org.apache.flink.api.scala.{DataSet, ExecutionEnvironment}
import org.apache.flink.api.scala._

object batchWordCount {
  def main(args: Array[String]): Unit = {
    // 创建环境
    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    // 读取数据
    val lines: DataSet[String] = env.readTextFile("./data/wordCount.txt")

    // flatMap
    val words: DataSet[String] = lines.flatMap(line => {
      line.split(" ")
    })

    //  map, groupBy, sum
    val res: AggregateDataSet[(String, Int)] = words.map((_, 1)).groupBy(0).sum(1)

    // print
    res.print()
  }
}
