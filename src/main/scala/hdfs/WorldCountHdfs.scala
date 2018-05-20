package hdfs

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.SparkContext._

/**
  * windows 环境 存在null\bin\winutils.exe问题
  */
object WorldCountHdfs {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("demo").setMaster("spark://c7:7077")
    val sc = new SparkContext(conf)
    // 读取hdfs数据
    val textFileRdd = sc.textFile("hdfs://c7:9000/test/word.txt")
    val fRdd = textFileRdd.flatMap { _.split(" ") }
    val mrdd = fRdd.map { (_, 1) }
    val rbkrdd = mrdd.reduceByKey(_+_, 1).sortBy(_._2,false)
                 //reduceByKey(_+_, 1) 代表合并一个文件输出
    //如果是reduceByKey(_+_)则输出多个文件分散在各个dataNode
    // 输出
    //rbkrdd.foreach(println) // ./spark-submit jar
    //运行： ./spark-submit --master spark://c7:7077 --class hdfs.WorldCount /usr/local/app/spark_demo.jar
    //结果查看： http://c7:8080/

    // 写入数据到hdfs系统
    rbkrdd.saveAsTextFile("hdfs://c7:9000/test/word_result")
  }
}
