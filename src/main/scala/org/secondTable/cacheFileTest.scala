package org.secondTable

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.FileUtils

import java.io.File
import scala.collection.mutable

object cacheFileTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 将文件注册到 env, 更改文件也无法更新内存
    env.registerCachedFile("./data/cityInfo.txt", "cityInfo")
    env.setParallelism(1)
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    stream.map(new RichMapFunction[String, String] {
      private val cityMap = new mutable.HashMap[String, String]()

      //每一个thread只会调用一次
      override def open(parameters: Configuration): Unit = {
        val file: File = getRuntimeContext.getDistributedCache.getFile("cityInfo")
        val strings: Array[String] = FileUtils.readFileUtf8(file).split("\n")
        for (str <- strings) {
          println(s"str: $str")
          val arrs = str.split(" ")
          val id = arrs(0)
          val city = arrs(1)
          cityMap.put(id, city)
        }
      }

      override def map(value: String): String = {
        cityMap.getOrElse(value, "not found city")
      }
    }).print()
    env.execute()
  }
}
