package org.table

import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.table.api.Table
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.types.Row

object createWindowSQL {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    val tableEnv: StreamTableEnvironment = StreamTableEnvironment.create(env)
    /*
    A 1000 success
    A 2000 failed
    A 3000 success
    A 4000 success
    A 5000 success
    A 6000 success
    A 7000 failed
     */
    val stream: DataStream[(String, Long, String)] = env.socketTextStream("localhost", 9999).map(row => {
      val arrs: Array[String] = row.split(" ")
      (arrs(0), arrs(1).toLong, arrs(2))
    }).assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[(String, Long, String)](Time.seconds(2)) {
      override def extractTimestamp(element: (String, Long, String)): Long = {
        element._2
      }
    })

    val table: Table = tableEnv.fromDataStream(stream, 'name, 'event_time.rowtime(), 'type)
    // Tumble Windows
    // Hop Windows
    // Cumulate Windows
    val result: Table = tableEnv.sqlQuery(s"select name, count(1), tumble_start(event_time, INTERVAL '5' seconds) as start_time, tumble_end(event_time, INTERVAL '5' seconds) as end_time " +
      s"from ${table} " +
      s"group by name, tumble(event_time, INTERVAL '5' seconds) ")
    tableEnv.toRetractStream[Row](result).print()
    env.execute()
  }
}
