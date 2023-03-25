package org.transformation

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable.ListBuffer

object mapOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
//    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)

    val stream: DataStream[Long] = env.fromSequence(1, 100)
    val mapStream: DataStream[String] = stream.map(x => x + "***")

    // 如何利用flatMap 代替 filter
    // 过滤数据中包含 "abc" 的数据
    val res: DataStream[String] = mapStream.flatMap(line => {
      val li = new ListBuffer[String]
      if (!line.contains("1")) {
        li += line
      }
      li.iterator
    })
    res.print()

    env.execute()
  }

}
