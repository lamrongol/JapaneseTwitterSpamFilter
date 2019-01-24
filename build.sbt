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
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".xml" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".types" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}