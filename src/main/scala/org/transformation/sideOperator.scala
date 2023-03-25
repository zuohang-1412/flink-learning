package org.transformation

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object sideOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[Long] = env.fromSequence(1, 100)

    // 设置侧输出流名称
    val outTag: OutputTag[Long] = new OutputTag[Long]("ji")
    val processStream: DataStream[Long] = stream.process(new ProcessFunction[Long,Long] {
      //处理每一个元素
      override def processElement(value: Long, ctx: ProcessFunction[Long, Long]#Context, out: Collector[Long]): Unit = {
        try {
          if (value %2 == 0) {
            //往主流发射
            out.collect(value)
          } else {
            //往测流发射
            ctx.output(outTag, value)
          }
        } catch {
          case e: Throwable => e.printStackTrace()
            ctx.output(outTag, value)
        }
      }
    })
    //获取测流数据
    val sideStream: DataStream[Long] = processStream.getSideOutput(outTag)
    sideStream.print("sideStream")
    //默认情况 print打印的是主流数据
    processStream.print("mainStream")
    env.execute()
  }
}
