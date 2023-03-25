package org.example

import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object carSpeed {
  case class carInfo(carId: String, speed:Long)
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 数据类型： a 123   a:车牌 123:速度
    val data: DataStream[String] = env.socketTextStream("localhost", 9999)
    data.map(e=>{
      val car: Array[String] = e.split(" ")
      carInfo(car(0), car(1).toLong)
    }).keyBy(_.carId).process(new KeyedProcessFunction[String, carInfo, String]{

      override def processElement(i: carInfo, context: KeyedProcessFunction[String, carInfo, String]#Context, collector: Collector[String]): Unit = {
        val currentTime: Long = context.timerService().currentProcessingTime()
        println(i.carId, i.speed)
        if (i.speed >120) {
          val timerTime: Long = currentTime + 2 * 1000
          context.timerService().registerProcessingTimeTimer(timerTime)
        }
      }

      override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, carInfo, String]#OnTimerContext, out: Collector[String]): Unit = {
        var warnMsg: String = "warn... time:" + timestamp + "  carID:" + ctx.getCurrentKey
        out.collect(warnMsg)
      }
    }).print()
    env.execute()
  }
}
