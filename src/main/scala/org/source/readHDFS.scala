package org.source

import org.apache.flink.api.java.io.TextInputFormat
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.functions.source.FileProcessingMode
import org.apache.flink.streaming.api.scala._

object readHDFS {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 一次性读文件
//    val data: DataStream[String] = env.readTextFile("hdfs://localhost:8020/flink/data/words")
    // 一直读取文件
    val filePath = "hdfs://localhost:8020/flink/data/words"
    val format = new TextInputFormat(new Path(filePath))
    val data: DataStream[String] = env.readFile(format, filePath, FileProcessingMode.PROCESS_CONTINUOUSLY, 10)

    val res: DataStream[(String, Int)] = data.flatMap(_.split(" ")).map((_, 1)).keyBy(0).sum(1)
    res.print()
    env.execute()
  }
}
