package org.example

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import redis.clients.jedis.Jedis

object wordCountRedis {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val cnt: DataStream[(String, Int)] = stream.flatMap(_.split(" ")).map((_, 1)).keyBy(0).sum(1)
    val res: DataStream[String] = cnt.map(new RichMapFunction[(String, Int), String] {
      var jedis: Jedis = _

      override def open(parameters: Configuration): Unit = {
        // 连接redis
        jedis = new Jedis("localhost", 6379)
        jedis.select(3)
      }

      // 处理每一个kv对
      override def map(in: (String, Int)): String = {
        jedis.set(in._1, in._2.toString)
      }

      override def close(): Unit = {
        jedis.close()
      }
    })
    res.print()
    env.execute()
  }
}
