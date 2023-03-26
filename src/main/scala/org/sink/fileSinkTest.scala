package org.sink

import org.apache.flink.api.common.serialization.SimpleStringEncoder
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._

import java.util.Base64.Encoder

object fileSinkTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)
    val res: DataStream[(String, Int)] = stream.flatMap(_.split(" "))
      .map((_, 1))
      .keyBy(0)
      .sum(1)
    // <IN, BucketID>    IN:写入的数据的类型  BucketID：桶的名称的类型  日期+小时
    val roll: DefaultRollingPolicy[(String, Int), String] = DefaultRollingPolicy.create()
      // 当文件大小超过256M 也会滚动产生一个小文件
      .withMaxPartSize(256 * 1024 * 1024)
      // 文件5s钟没有写入新的数据，那么会产生一个新的小文件（滚动）
      .withInactivityInterval(5000)
      // 文件打开时间 超过10s 也会滚动产生一个小文件
      .withRolloverInterval(10000)
      .build()
    val sink: StreamingFileSink[(String, Int)] = StreamingFileSink
      .forRowFormat(new Path("./data/fileSink"), new SimpleStringEncoder[(String, Int)]("UTF-8"))
      // 每隔1 秒检查 桶中的数据
      .withBucketCheckInterval(1000)
      .withRollingPolicy(roll)
      .build()


    res.addSink(sink)
    env.execute()
  }
}
