package org.source

import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}

import scala.util.Random

// 自定义数据源
object CustomSource {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //[String] 为发送数据源类型 ； SourceFunction 只支持单线程发送数据源
    env.addSource(new SourceFunction[String] {
      var flag = true
      override def run(sourceContext: SourceFunction.SourceContext[String]): Unit = {
        val random = new Random()
        while(true) {
          // collect 中加入 redis 查询出来的数据
          sourceContext.collect("hello "+ random.nextInt(100))
          Thread.sleep(500)
        }
      }

      override def cancel(): Unit = {
        flag = false
      }
    })
  }
}
