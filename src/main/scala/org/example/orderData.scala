package org.example

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.util.{Collector}
import org.apache.flink.streaming.api.scala._

/*
业务场景：
订单创建， 超时15分钟未支付，提示用户：账单未支付
订单完成， 提示商家，订单已完成
 */

// 订单格式
case class orderInfo(oid: Int, oType: String, otime: Long)

// 消息格式
case class MsgInfo(oid: Int, msg: String, createTime: Long, payTime: Long)

object orderData {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    val data: DataStream[String] = env.readTextFile("./data/orderData.txt")

    // 创建侧输出流
    val outTag: OutputTag[MsgInfo] = new OutputTag[MsgInfo]("time_out")
    val stream: DataStream[orderInfo] = data.map(line => {
      val arrs: Array[String] = line.split(",")
      new orderInfo(arrs(0).toInt, arrs(1), arrs(2).toLong)
    }).assignAscendingTimestamps(_.otime)

    val result: DataStream[MsgInfo] = stream.keyBy(_.oid)
      .process(new KeyedProcessFunction[Int, orderInfo, MsgInfo] {
        // 设置 value state
        var createOrderState: ValueState[orderInfo] = _
        // 设置 time state
        var timeOutState: ValueState[Long] = _

        override def open(parameters: Configuration): Unit = {
          val create_order: ValueStateDescriptor[orderInfo] = new ValueStateDescriptor[orderInfo]("create_order", classOf[orderInfo])
          createOrderState = getRuntimeContext.getState(create_order)
          val time_out: ValueStateDescriptor[Long] = new ValueStateDescriptor[Long]("time_out", classOf[Long])
          timeOutState = getRuntimeContext.getState(time_out)

        }

        override def processElement(value: orderInfo, ctx: KeyedProcessFunction[Int, orderInfo, MsgInfo]#Context, out: Collector[MsgInfo]): Unit = {
          val info: orderInfo = createOrderState.value()
          if (value.oType.equals("create") && info == null) {
            // 这是一个创建订单数据，并且启动定时器（15分钟推送消息）
            createOrderState.update(value)
            val ts: Long = value.otime + 9000L
            timeOutState.update(ts)
            // 启动触发器
            ctx.timerService().registerEventTimeTimer(ts)
          } else if (value.oType.equals("pay") && info != null) {
            var str:String = ""
            if (value.otime < timeOutState.value()) {
              ctx.timerService().deleteEventTimeTimer(timeOutState.value())
              str = "15分钟内订单已完成，请尽快发货"
            }else{
              str = "订单已完成，请尽快发货"
            }
            val msg = MsgInfo(value.oid, str, info.otime, value.otime)
            // 主流发送数据
            out.collect(msg)
          } else {
            // 删除
            createOrderState.clear()
            timeOutState.clear()
          }
        }

        // 触发器触发方法
        override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Int, orderInfo, MsgInfo]#OnTimerContext, out: Collector[MsgInfo]): Unit = {
          val order: orderInfo = createOrderState.value()
          val msg = new MsgInfo(order.oid, "您的订单还未支付。。。", order.otime, 0)
          // 侧流发送数据
          ctx.output(outTag, msg)
        }
      })
    result.print("主流")
    result.getSideOutput(outTag).print("侧流")
    env.execute()
  }
}
