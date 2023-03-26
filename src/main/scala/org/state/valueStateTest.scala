package org.state

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._

object valueStateTest {
  case class CarInfo(carId:String, speed:Long)
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // AAA 10; AAA 100; AAA 120; BBB 10; BBB 50; BBB 100
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    stream.map(x => {
      val arrs: Array[String] = x.split(" ")
      CarInfo(arrs(0), arrs(1).toLong)
    }).keyBy(_.carId)
      .map(new RichMapFunction[CarInfo, String] {
        private var lastTempSpeed: ValueState[Long] = _

        override def open(parameters: Configuration): Unit = {
          val desc = new ValueStateDescriptor[Long]("lastSpeed", createTypeInformation[Long])
          lastTempSpeed = getRuntimeContext.getState(desc)
        }

        override def map(value: CarInfo): String = {
          val lastSpeed = lastTempSpeed.value()
          this.lastTempSpeed.update(value.speed)
          if (lastSpeed != 0 && value.speed - lastSpeed > 30) {
            "over speed " + value.toString
          } else {
            s"${value.carId}, ${lastSpeed.toString}, ${value.speed.toString}"
          }
        }
      }).print()
    env.execute()
  }
}
