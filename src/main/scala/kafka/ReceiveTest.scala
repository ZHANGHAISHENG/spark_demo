package kafka

import org.apache.kafka.common.serialization.{IntegerDeserializer, StringDeserializer}
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

/**
  * ./spark-submit --master spark://c7:7077 --class kafka.ReceiveTest /usr/local/app/spark_demo.jar
  * 访问地址： http://c7:8080
  *
  * 注意问题：
  * kafka server.properties 需要配置：
  * listeners=PLAINTEXT://192.168.0.104:9092 ， 不能够是PLAINTEXT://:9092
  * 否则会出现： rack: null) dead for group 错误
  *
  * 参考： https://segmentfault.com/a/1190000011022181
  *
  */
object ReceiveTest {

  def main(args: Array[String]): Unit = {
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "192.168.0.104:9092",
      "key.deserializer" -> classOf[IntegerDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "DemoConsumer",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val conf = new SparkConf().setAppName("demo").setMaster("spark://c7:7077")
    val ssc = new StreamingContext(conf, Seconds(4))

    val topics = Array("topic1")
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )
    val r = stream.map(record => (record.key, record.value))
    // stream.map(record => (record.key, record.value)).print()  // 在master 输出
    r.foreachRDD { x =>
      println("receive start: ----------------------------------------------") //  在master 输出
      x.foreachPartition(y => { // 在slave 输出
        if(y.isEmpty){
          println("receive empty data: "+ y)
        } else {
          y.foreach(item => println("receive:" +item))
        }
      })
      println("receive end: ------------------------------------------------")
    }
    ssc.start()
    ssc.awaitTermination()
  }

}
