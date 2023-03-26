package org.sink

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import java.sql.{Connection, DriverManager, PreparedStatement}


object mysqlSinkTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val res: DataStream[(String,Int)] = stream.flatMap(_.split(" "))
      .map((_, 1))
      .keyBy(0)
      .sum(1)
    res.addSink(new RichSinkFunction[(String, Int)] {
      var conn: Connection = _
      var updatePst: PreparedStatement = _
      var insertPst: PreparedStatement = _
      override def open(parameters: Configuration): Unit = {
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/data", "root", "zuohang123")
        updatePst = conn.prepareStatement("update wordCount set cnt = ? where word = ?")
        insertPst = conn.prepareStatement("insert into wordCount values(?,?)")
      }

      override def close(): Unit = {
        conn.close()
        updatePst.close()
        insertPst.close()
      }

      override def invoke(value: (String, Int)): Unit = {
        println(value._1,value._2)
        updatePst.setString(2, value._1)
        updatePst.setInt(1, value._2)
        updatePst.execute()
        if (updatePst.getUpdateCount == 0) {
          insertPst.setString(1, value._1)
          insertPst.setInt(2, value._2)
          insertPst.execute()
        }
      }
    })
    env.execute()
  }
}
