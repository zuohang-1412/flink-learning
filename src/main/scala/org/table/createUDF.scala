package org.table

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.Table
import org.apache.flink.table.api._
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.functions.TableFunction
import org.apache.flink.types.Row


object createUDF {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val TableEnv: StreamTableEnvironment = StreamTableEnvironment.create(env)
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val table: Table = TableEnv.fromDataStream(stream, 'line)
    val udf = new WCudf
    val result: Table = table.flatMap(udf('line))
      .as('word, 'word_count)
      .groupBy('word)
      .select('word, 'word_count.sum as 'cnt)
    TableEnv.toRetractStream[Row](result).print()
    env.execute()
  }

  class WCudf extends TableFunction[Row] {
    // 返回类型定义
    override def getResultType: TypeInformation[Row] = {
      Types.ROW(Types.STRING, Types.INT)
    }

    // 主体
    def eval(line: String): Unit = {
      line.split(" ").foreach(word => {
        val row = new Row(2)
        row.setField(0, word)
        row.setField(1, 1)
        collect(row)
      })
    }
  }
}
