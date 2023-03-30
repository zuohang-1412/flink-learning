package org.table

import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, Table}
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.sources.CsvTableSource


object createTable {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val settings: EnvironmentSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inBatchMode().build()
    val tableEnv: StreamTableEnvironment = StreamTableEnvironment.create(env, settings)

    val source: CsvTableSource = new CsvTableSource(
      "./data/scores.txt",
      Array[String]("id", "name", "score"),
      Array(Types.INT, Types.STRING, Types.DOUBLE)
    )
    val table: Table = tableEnv.fromTableSource(source)
    tableEnv.registerTable("scores", table)
    tableEnv.scan("scores").printSchema()
  }
}
