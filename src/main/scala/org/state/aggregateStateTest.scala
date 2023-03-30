package org.state

import org.apache.flink.api.common.functions.{AggregateFunction, ReduceFunction, RichMapFunction}
import org.apache.flink.api.common.state.{AggregatingState, AggregatingStateDescriptor, ReducingState, ReducingStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._

object aggregateStateTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 车牌号 速度
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    stream.map(row => {
      val arrs: Array[String] = row.split(" ")
      (arrs(0), arrs(1).toLong)
    }).keyBy(_._1)
      .map(new RichMapFunction[(String, Long), (String, Long)] {
        // 速度求和
        private var speedCount: AggregatingState[Long, Long] = _

        override def open(parameters: Configuration): Unit = {
          val desc = new AggregatingStateDescriptor[Long, Long, Long]("agg", new AggregateFunction[Long, Long, Long] {
            //初始化一个累加器
            override def createAccumulator(): Long = 0

            //每来一条数据会调用一次
            override def add(value: Long, accumulator: Long): Long = accumulator + value

            override def getResult(accumulator: Long): Long = accumulator

            override def merge(acc1: Long, acc2: Long): Long = acc1 + acc2
          }, createTypeInformation[Long])
          speedCount = getRuntimeContext.getAggregatingState(desc)
        }

        override def map(value: (String, Long)): (String, Long) = {
          speedCount.add(value._2)
          (value._1, speedCount.get())
        }
      }).print()
    env.execute()
  }
}
