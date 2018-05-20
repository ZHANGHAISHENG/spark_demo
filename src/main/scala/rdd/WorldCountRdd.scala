package rdd

import org.apache.spark.{SparkConf, SparkContext}

/**
  * windows 环境 存在null\bin\winutils.exe问题
  */
object WorldCountRdd {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("demo").setMaster("spark://c7:7077")
    val sc = new SparkContext(conf)
    val num=sc.parallelize(1 to 10) //将数组并行化成RDD，默认分片
    val doublenum=num.map(_*2)  //每个元素*2
    doublenum.foreach(println)
  }
}
