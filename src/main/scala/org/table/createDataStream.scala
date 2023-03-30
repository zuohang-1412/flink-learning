package org.table

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.bridge.scala.{StreamTableEnvironment, tableConversions}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.Table
import org.apache.flink.table.api._
import org.apache.flink.types.Row

object createDataStream {
  case class Scores(id:Int, name:String, score:Int)
  def main(args: Array[String]): Unit = {
    val StreamEnv: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val tableEnv: StreamTableEnvironment = StreamTableEnvironment.create(StreamEnv)
    val stream: DataStream[String] = StreamEnv.socketTextStream("localhost", 9999)
    val data: DataStream[Scores] = stream.map(line => {
      val arrs: Array[String] = line.split(" ")
      new Scores(arrs(0).toInt, arrs(1), arrs(2).toInt)
    })
    val table: Table = tableEnv.fromDataStream(data) // 默认表名为 id, name, score
//    tableEnv.fromDataStream(data, 'p_id, 'p_name, 'p_score) 重新命名
    table.printSchema()
    val res: Table = table.select('id, 'name, 'score)
    tableEnv.toRetractStream[Row](res).print()
    StreamEnv.execute()
  }
}
