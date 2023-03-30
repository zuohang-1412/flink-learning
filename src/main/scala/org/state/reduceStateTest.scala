package org.state

import org.apache.flink.api.common.functions.{ReduceFunction, RichMapFunction}
import org.apache.flink.api.common.state.{ReducingState, ReducingStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._

object reduceStateTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 车牌号 速度
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    stream.map(row=> {
      val arrs: Array[String] = row.split(" ")
      (arrs(0), arrs(1).toLong)
    }).keyBy(_._1)
      .map(new RichMapFunction[(String,Long), (String, Long)] {
        // 测算车辆最大时速
        private var maxSpeed: ReducingState[Long] = _
        override def open(parameters: Configuration): Unit = {
          val desc = new ReducingStateDescriptor[Long](
            "maxSpeed",
            new ReduceFunction[Long] {
              override def reduce(value1: Long, value2: Long): Long = math.max(value1, value2)
            },
            createTypeInformation[Long]
          )
          maxSpeed = getRuntimeContext.getReducingState(desc)
        }

        override def map(value: (String, Long)): (String, Long) = {
          maxSpeed.add(value._2)
          (value._1, maxSpeed.get())
        }
      }).print()
    env.execute()
  }
}
