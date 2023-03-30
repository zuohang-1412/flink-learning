package org.cepEg

import org.apache.flink.cep.PatternSelectFunction
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/*
 pattern API 流程：
 1. 输入事件流的创建
 2. Parttern 的定义
 3. Parttern 应用在数据流流上检测
 4. 选取结果
 */
case class loginEvent(userName: String, eventTime: Long, EventType: String)

object createCEP {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    // 1
    val stream: DataStream[loginEvent] = env.socketTextStream("localhost", 9999).map(row => {
      val arrs: Array[String] = row.split(" ")
      new loginEvent(arrs(0), arrs(1).toLong, arrs(2))
    }).assignAscendingTimestamps(_.eventTime)

    // 2
    val pat: Pattern[loginEvent, loginEvent] = Pattern.begin[loginEvent]("start")
      .where(_.EventType.equals("failed"))
      .times(3)
      .greedy
      .within(Time.seconds(10))

    // 3
    val ps: PatternStream[loginEvent] = CEP.pattern(stream.keyBy(_.userName), pat)

    // 4
    val result: DataStream[String] = ps.select(
      patternSelectFun = (pattern: collection.Map[String, Iterable[loginEvent]]) => {
        val bus = new StringBuilder()
        val list: List[loginEvent] = pattern.get("start").get.toList
        bus.append(list(0).userName).append(" 恶意登录： ")
        for (i <- 0 until list.size) {
          bus.append(s"${list(i).userName} ${list(i).eventTime} ${list(i).EventType};")
        }
        bus.toString()
      }
    )
    result.print()
    env.execute()
  }
}
