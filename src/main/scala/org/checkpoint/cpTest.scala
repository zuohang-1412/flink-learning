package org.checkpoint

import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._

object cpTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    // 数据保存位置
    env.setStateBackend(new FsStateBackend("hdfs://localhost:8020/flink/checkpoint",true))
    // 代表每隔1000ms 往数据源中插入一个barrier
    env.enableCheckpointing(1000)
    // 失败重试次数
    env.getCheckpointConfig.setTolerableCheckpointFailureNumber(2)
    // 设置超时时间 5min 默认：10min
    env.getCheckpointConfig.setCheckpointTimeout(5*60*1000)
    // 设置checkpoint 模式
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
    // 设置job 并行数
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
    // 如果并行数为1，才可以设置两个job 之间的间隔时间：600ms
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(600)
    // 当任务取消时， checkpoint 是否存储
    // RETAIN_ON_CANCELLATION 存储
    // DELETE_ON_CANCELLATION 删除
    env.getCheckpointConfig.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)

    val stream = env.socketTextStream("localhost", 8888)
    stream.flatMap(_.split(" "))
      .map((_, 1))
      .keyBy(_._1)
      .reduce((v1: (String, Int), v2: (String, Int)) => {
        (v1._1, v1._2 + v2._2)
      })
      .print()
    env.execute()
  }
}
