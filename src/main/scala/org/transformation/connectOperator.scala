package org.transformation

import org.apache.flink.streaming.api.functions.co.CoMapFunction
import org.apache.flink.streaming.api.scala.{ConnectedStreams, DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._

object connectOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val st1: DataStream[String] = env.socketTextStream("localhost", 9999)
    val st2: DataStream[String] = env.socketTextStream("localhost", 8888)
    val stream1: DataStream[(String, Int)] = st1.flatMap(_.split(" ")).map((_, 1)).keyBy(0).sum(1)
    val stream2: DataStream[(String, Int)] = st2.flatMap(_.split(" ")).map((_, 1)).keyBy(0).sum(1)

    // 可以用于 DataStream 不同的情况。
    val con: ConnectedStreams[(String, Int), (String, Int)] = stream2.connect(stream1)
    // 输出类型必须一致
    con.map(new CoMapFunction[(String, Int), (String, Int), (String, Int)]{
      // 处理 stream2 的元素
      override def map1(in1: (String, Int)): (String, Int) = {
        println("in1: ", in1)
        in1
      }
      // 处理 stream1 的元素
      override def map2(in2: (String, Int)): (String, Int) = {
        println("in1: ", in2)
        in2
      }
    })
    env.execute()
  }
}
