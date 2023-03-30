package org.secondTable

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import java.util.{Timer, TimerTask}
import scala.collection.mutable
import scala.io.Source

object TimeQueryTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    stream.map(new RichMapFunction[String, String] {
      private val mapInfo = new mutable.HashMap[String, String]()

      def query(): Unit = {
        // 可以修改文件内容
        val source = Source.fromFile("./data/cityInfo.txt", "UTF-8")
        val iterator = source.getLines()
        for (elem <- iterator) {
          val vs = elem.split(" ")
          mapInfo.put(vs(0), vs(1))
        }
      }

      // 执行query 调用
      override def open(parameters: Configuration): Unit = {
        query()
        val timer: Timer = new Timer(true)
        timer.schedule(new TimerTask {
          override def run(): Unit = {
            query()
          }
        },
          // 一秒后， 每2秒执行一次 TimerTask
          1000,
          2000
        )
      }

      override def map(value: String): String = {
        mapInfo.getOrElse(value, "not found city")
      }
    }).print()
    env.execute()
  }
}
