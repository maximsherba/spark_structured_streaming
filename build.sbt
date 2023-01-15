name := "StructuredStreamingKafka"

version := "1.0"

scalaVersion := "2.12.12"

lazy val sparkVersion = "3.3.1"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" % "spark-sql_2.12" % sparkVersion, // % "provided",
  "org.apache.spark" % "spark-streaming_2.12" % sparkVersion, // % "provided",
  "org.apache.spark" % "spark-sql-kafka-0-10_2.12" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided",
)

assemblyMergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf")       => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$")   => MergeStrategy.discard
  case "reference.conf"                                 => MergeStrategy.concat
  case x: String if x.contains("UnusedStubClass.class") => MergeStrategy.first
  case _                                                => MergeStrategy.first
}