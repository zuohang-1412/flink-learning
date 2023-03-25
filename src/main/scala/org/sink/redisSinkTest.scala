package org.sink

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

object redisSinkTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)

    val config: FlinkJedisPoolConfig = new FlinkJedisPoolConfig.Builder().setHost("localhost").setPort(6379).setDatabase(3).build()

    stream.flatMap(line => {
      val rest = new ListBuffer[(String, Int)]
      line.split(" ").foreach(word => rest += ((word, 1)))
      rest
    }).keyBy(_._1)
      .reduce((v1: (String, Int), v2: (String, Int)) => {
        (v1._1, v1._2 + v2._2)
      }).addSink(new RedisSink(config, new RedisMapper[(String, Int)] {
      // 指定操作Redis的命令
      // todo 这个位置报错 无法更新key (可能是maven 中redis冲突导致，我把redis.clients 改到3.2.0 就可以了。。。)
      override def getCommandDescription: RedisCommandDescription = {
        new RedisCommandDescription(RedisCommand.HSET, "wordCount")
      }

      override def getKeyFromData(data: (String, Int)): String = {
        data._1
      }

      override def getValueFromData(data: (String, Int)): String = {
        data._2.toString
      }
    }))

    env.execute()
  }
}
