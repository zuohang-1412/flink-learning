package org.source

import org.apache.flink.streaming.api.functions.source.{ParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._

import scala.util.Random

object parallelSource {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.addSource(new ParallelSourceFunction[String] {
      var flag = true

      override def run(sourceContext: SourceFunction.SourceContext[String]): Unit = {
        val random = new Random()
        while (true) {
          // collect 中加入 redis 查询出来的数据
          sourceContext.collect("hello " + random.nextInt(100))
          Thread.sleep(500)
        }
      }

      override def cancel(): Unit = {
        flag = false
      }
    }).setParallelism(2)

  }
}
