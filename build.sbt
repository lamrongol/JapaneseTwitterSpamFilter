name := "SpamFilter"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "1.5.1"
libraryDependencies += "org.apache.spark" % "spark-mllib_2.11" % "1.5.1"
libraryDependencies += "com.atilika.kuromoji" % "kuromoji-ipadic" % "0.9.0"
libraryDependencies += "com.twitter" % "twitter-text" % "1.13.0"
libraryDependencies += "org.twitter4j" % "twitter4j-core" % "4.0.4"
libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "4.0.4"

//http://www.mwsoft.jp/programming/scala/scala_logging.html
libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

