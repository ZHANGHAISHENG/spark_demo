说明：
yarn-client相当于是命令行 会将你输入的代码提交到yarn上面执行 yarn-cluster是将你写好的程序打成jar包然后提交到yarn上面去执行 然后yarn会将jar包分发到各个节点 并负责资源分配和任务管理

rack: null) dead for group 错误 参考：
kafka server.properties 需要配置： listeners=PLAINTEXT://192.168.0.104:9092 ， 不能够是PLAINTEXT://:9092
参考： https://segmentfault.com/a/1190000011022181

 spark on yarn 的支持两种模式
    1）yarn-cluster：适用于生产环境；
    2）yarn-client：适用于交互、调试，希望立即看到app的输出


spark on yarn 开发环境搭建:
集群规划：（不启动HA）
	主机名		IP		      安装的软件				     运行的进程
	c7             192.168.0.107       jdk、scala、hadoop、spark           nameNode、ResourceManager、Master
	c8             192.168.0.108       jdk、scala、hadoop、spark           SecondaryNameNode、DataNode、NodeManager、Worker
	c9             192.168.0.109       jdk、scala、hadoop、spark           DataNode、NodeManager、Worker



环境变量配置：
下载jdk、scala 解压到/usr/java/下

/etc/profile：
export HADOOP_HOME=/usr/local/app/hadoop-2.7.6
export JAVA_HOME=/usr/java/jdk1.8.0_171
export PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
export HADOOP_COMMON_LIB_NATIVE_DIR=$HADOOP_HOME/lib/native
export HADOOP_OPTS="-Djava.library.path=$HADOOP_HOME/lib:$HADOOP_COMMON_LIB_NATIVE_DIR"
export SCALA_HOME=/usr/java/scala-2.12.6
export PATH=$PATH:$SCALA_HOME/bin
-- source /etc/profile  #生效环境变量

hadoop配置：
目录: /usr/local/app/hadoop-2.7.6
etc\hadoop\hadoop-env.sh:
export JAVA_HOME=/usr/java/jdk1.8.0_171
etc\hadoop\yarn-env.sh:
export JAVA_HOME=/usr/java/jdk1.8.0_171
etc\hadoop\slaves:
c8
c9

core-site.xml：
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://c7:9000/</value>
    </property>
    <property>
        <name>hadoop.tmp.dir</name>
        <value>/usr/local/app/hadoop-2.7.6/tmp</value>
    </property>
</configuration>

hdfs-site.xml：
<configuration>
    <property>
        <name>dfs.namenode.secondary.http-address</name>
        <value>c8:50090</value>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>/usr/local/app/hadoop-2.7.6/dfs/name</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>/usr/local/app/hadoop-2.7.6/dfs/data</value>
    </property>
    <property>
        <name>dfs.replication</name>
        <value>3</value>
    </property>
</configuration>

mapred-site.xml：
<configuration>
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>
</configuration>

yarn-site.xml：
<configuration>
<!-- Site specific YARN configuration properties -->
      <property>
          <name>yarn.nodemanager.aux-services</name>
          <value>mapreduce_shuffle</value>
      </property>
      <property>
          <name>yarn.resourcemanager.hostname</name>
          <value>c7</value>
      </property>
</configuration>

spark配置：
目录： /usr/local/app/spark-2.3.0-bin-hadoop2.7
conf\spark-env.sh:
export JAVA_HOME=/usr/java/jdk1.8.0_171
export SCALA_HOME=/usr/java/scala-2.12.6
export HADOOP_CONF_DIR=/usr/local/app/hadoop-2.7.6/etc/hadoop
export YARN_CONF_DIR=/usr/local/app/hadoop-2.7.6/etc/hadoop
export SPARK_MASTER_HOST=c7

conf\slaves:
c8
c9

-- 将hadoop-2.7.6配置好复制到c7、c8、c9

首次需要格式化master 的 nameNode:
c7执行： bin/hadoop namenode -format    #格式化namenode

启动\关闭：
hadoop: sbin\start-all.sh
spark: sbin\start-all.sh

hadoop: sbin\stop-all.sh
spark: sbin\stop-all.sh

监控：
hadoop:
  集群状态：  http://c7:50070
  应用运行状态： http://c7:8088

spark:
 集群状态：http://c7:8080/
 应用状态： http://c7:7077  (提交了jar之后才能访问)


其他配置：
-- 可参考hadoop开发环境搭建


spark shell 操作hdfs:
启动spark shell:/bin/spark-shell --master spark://c7:8080
c7 可以直接： ./spark-shell

给hadoop添加数据：
hadoop fs -mkdir  /test
hadoop put /usr/local/app/tmp/word.txt /test

spark-shell执行：
val s = sc.textFile("hdfs://c7:9000/test/word.txt")
val counts =  s.flatMap(line => line.split(" ")).map(word => (word, 1)).reduceByKey(_ + _)
counts.foreach(println)
参考： 
hdfs 单词统计：https://blog.csdn.net/zhaoyunxiang721/article/details/49173737
spark读取hdfs上的文件和写入数据到hdfs上面: https://www.cnblogs.com/heml/p/6186109.html
spark各种数据源： https://www.cnblogs.com/caiyisen/p/7527459.html


提交运行jar:
./spark-submit --master spark://c7:7077 --class kafka.ReceiveTest2 /usr/local/app/spark_demo.jar
idea 打包时候不需要引入任何包

-- 如果是集成kafka，需要将spark-streaming-kafka-0-10_2.11-2.3.0.jar、kafka-clients-0.10.0.1.jar 
加到c7、c8、c9  /usr/local/app/spark-2.3.0-bin-hadoop2.7/jars 目录下  （理论上idea打包时候将这两个包打入即可）

网络配置：

网络配置固定ip(包括桥接)：
/etc/sysconfig/network-scripts/ifcfg-eth0
E=eth0
TYPE=Ethernet
ONBOOT=yes
NM_CONTROLLED=yes
BOOTPROTO=static
HWADDR=00:0C:29:E5:EF:BD （默认即可，可以不填写，一般只有一个物理网卡）
IPADDR=192.168.0.108
NETMASK=255.255.255.0
GATEWAY=192.168.0.1
DNS1=8.8.8.8
IPV6INIT=no
USERCTL=no  # 配置了这个，就不能配置PREFIX=24

或：

DEVICE=eth0
TYPE=Ethernet
ONBOOT=yes
NM_CONTROLLED=yes
BOOTPROTO=static
#HWADDR=00:0c:29:e5:ef:bd
IPADDR=192.168.0.108
PREFIX=24
GATEWAY=192.168.0.1
DNS1=218.85.157.99
DNS2=8.8.8.8
DEFROUTE=yes
IPV4_FAILURE_FATAL=yes
IPV6INIT=no
NAME="System eth0"


查看网卡信息(不区分大小写)：
ifconfig ：  HWaddr 00:0C:29:E5:EF:BD
ip addr:  link/ether 00:0c:29:e5:ef:bd
cat : /etc/udev/rules.d/70-persistent-net.rules  (每次启动都会从虚拟机读取并写入)

nat 方式相关:
centos网络重要配置文件：
/etc/sysconfig/network-scripts/ifcfg-eth0
/etc/udev/rules.d/70-persistent-net.rules

IP地址分两部分：网络地址+主机地址
网络地址： 192.168.1.0, 网关255.255.255.0  (网络地址最后一位一定是0)
https://jingyan.baidu.com/article/2c8c281d8ae38a0009252a6b.html

CentOS网卡配置出现device not managed by networkmanager:
关闭NetworkManager（service NetworkManager stop）
重启network（service network restart）
再启动NetworkManager（service NetworkManager start）

nat 方式ping 不同，检查是否能ping通网关，如果不能，检查 window nat service 是否有启动。
（本次实验，vmware nat服务安装失败，可能是软件冲突）

centos6.5 nat网络配置： https://blog.csdn.net/zh_666888/article/details/78567326


---------------------------------------------------------------------------------------------------------------
相关参考文档：
---------------------------------------------------------------------------------------------------------------


Spark Streaming与Storm的对比分析:
https://blog.csdn.net/kwu_ganymede/article/details/50296831


yarn:
 https://www.cnblogs.com/wcwen1990/p/6737985.html
 job: https://blog.csdn.net/suifeng3051/article/details/49486927

hadoop:
整体：https://blog.csdn.net/yywusuoweile/article/details/47678897
hdfs: https://www.cnblogs.com/laov/p/3434917.html
secondary nameName：https://www.cnblogs.com/thinkpad/p/5173705.html
fsimage与editlog：https://blog.csdn.net/chenkfkevin/article/details/61196409
mapreduce: https://blog.csdn.net/tanggao1314/article/details/51275812(hadoop2)
                    https://blog.csdn.net/yybk426/article/details/76601921（hadoop1）
shuffle: http://langyu.iteye.com/blog/992916
ha:
     深入理解:http://www.aboutyun.com/thread-22935-1-1.html
     原理详解:https://www.cnblogs.com/sy270321/p/4398815.html
     hadoop2.xFederation:  https://www.2cto.com/database/201709/677231.html
                      federation 配置： https://www.cnblogs.com/meiyuanbao/p/3545929.html
     kill nameNode补刀： https://blog.csdn.net/a731107548/article/details/54378080

路线：https://jingyan.baidu.com/article/cd4c2979f8f97a756e6e6006.html

spark: https://www.cnblogs.com/tgzhu/p/5818374.html
       https://www.cnblogs.com/liuliliuli2017/p/6809094.html   
 
spark Standalone 高可用：
https://www.cnblogs.com/byrhuangqiang/p/3937654.html

spark on yarn开发环境搭建：
  https://blog.csdn.net/u010638969/article/details/51283216
  https://www.cnblogs.com/zengxiaoliang/p/6478859.html
  https://www.cnblogs.com/codingexperience/p/5333202.html
  https://www.cnblogs.com/zishengY/p/6819160.html?utm_source=itdadao&utm_medium=referral （hadoop）

spark stream :  
  window 操作： https://www.cnblogs.com/duanxz/p/4408789.html
 
Kafka+Spark Streaming+Redis:
  http://shiyanjun.cn/archives/1097.html

mysql 分表分库：
https://www.cnblogs.com/try-better-tomorrow/p/4987620.html
用户角度、商户角度例子：https://www.cnblogs.com/huanxiyun/articles/5586813.html
分片数据放到临时表进行聚合：https://blog.csdn.net/carechere/article/details/51211236
深度分页：elaticsearch 搜索引擎解决方案


