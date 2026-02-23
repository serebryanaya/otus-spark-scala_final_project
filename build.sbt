ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    name := "project_final",
    assembly / mainClass := Some("LogProcessor"),
    assembly / assemblyJarName := "project_final-assembly.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case "reference.conf" => MergeStrategy.concat
      case x => MergeStrategy.first
    }
  )

val sparkVersion = "3.5.5"
lazy val log4jVersion = "2.22.1"
lazy val postgresVersion = "42.6.0"
lazy val kafkaVersion = "3.4.1"
lazy val clickhouseVersion = "0.7.2"

lazy val jacksonVersion = "2.15.2"
lazy val json4sVersion = "3.7.0-M11"
lazy val log4jScalaVersion = "13.0.0"
//lazy val log4jScalaVersion = "13.1.0"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided", // TO DO надо ли????
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "org.postgresql" % "postgresql" % postgresVersion,


//  "com.github.housepower" % "clickhouse-native-jdbc" % "2.7.0"
//  "com.clickhouse" % "clickhouse-jdbc" % "0.9.5" classifier "all",
  "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.1.54"
//  "com.clickhouse.spark" %% "clickhouse-spark-runtime-3.5" % "0.10.0"
//  "org.apache.logging.log4j" %% "log4j-api-scala" % log4jScalaVersion,
//  "org.apache.logging.log4j" %% "log4j-api" % log4jVersion,
//  "org.apache.logging.log4j" %% "log4j-core" % log4jVersion,

//  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
//  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
//  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
//  "org.json4s" %% "json4s-jackson" % json4sVersion

//
//
//
////
//"org.slf4j" % "slf4j-api" % "2.0.9"
//,
//"org.slf4j" % "slf4j-simple" % "2.0.9"

)
//
//libraryDependencies += "com.clickhouse" % "clickhouse-jdbc" % "0.9.5" classifier "all"
//libraryDependencies += "com.clickhouse.spark" %% "clickhouse-spark-runtime-3.5.5_2.12" % "0.10.0"
//
//

