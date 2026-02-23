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
lazy val clickhouseVersion = "0.1.54"


libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"
    exclude("com.google.guava", "guava"),
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
    exclude("com.google.guava", "guava"),
  "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided"
    exclude("com.google.guava", "guava"),
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion
    exclude("com.google.guava", "guava"),
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion
    exclude("com.google.guava", "guava"),
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "org.postgresql" % "postgresql" % postgresVersion,

  "ru.yandex.clickhouse" % "clickhouse-jdbc" % clickhouseVersion
//  "com.google.guava" % "guava" % "33.0.0-jre"
)
