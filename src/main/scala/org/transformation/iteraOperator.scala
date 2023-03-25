package org.transformation

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
object iteraOperator {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val data: DataStream[Long] = env.fromSequence(1, 100)
    val res: DataStream[Long] = data.iterate(
      iteration => {
        val iterationBody: DataStream[Long] = iteration.map(x => {
          println(s"value: $x")
          if (x % 2 == 1) {
            x - 1
          } else {
            x
          }
        })
        // 当为奇数时会重新
        (iterationBody.filter(_ % 2 == 1), iterationBody.filter(_ % 2 == 0))
      }
    )
    res.print()
    env.execute()
  }

}
