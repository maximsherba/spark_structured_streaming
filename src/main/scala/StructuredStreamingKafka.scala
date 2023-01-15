import org.apache.spark.SparkConf
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, struct, to_json}

import java.util.UUID

object StructuredStreamingKafka {
  def main(args: Array[String]): Unit = {
    if (args.length < 6) {
      System.err.println("Usage: StructuredKafkaWordCount <spark-master url> <path to ML model> <bootstrap-servers> " +
        "<subscribe-type> <topic in> <topic out> [<checkpoint-location>]")
      System.exit(1)
    }

    val Array(sparkMaster, pathToModel, bootstrapServers, subscribeType, topicIn, topicOut, _*) = args
    val checkpointLocation =
      if (args.length > 6) args(6) else "/tmp/temporary-" + UUID.randomUUID.toString

    val sparkConf = new SparkConf()
      .setAppName("StructuredKafkaWordCount")
      .setMaster(sparkMaster) //"spark://master:7077"

    val spark = SparkSession
      .builder
      .config(sparkConf)
      .getOrCreate()

    import spark.implicits._

    // Create DataSet representing the stream of input lines from kafka
    val input = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServers)
      //.option("group.id", "homework")
      .option("startingOffsets", "earliest")
      //.option("enable.auto.commit","false")
      .option(subscribeType, topicIn)
      .load()
      .selectExpr("CAST(value AS STRING)")
      .as[String].map(_.split(","))
      .map(Model(_))

    // Применяем модель к входным данным
    val model = PipelineModel.load(pathToModel) //"/spark/Scala/IrisModel"
    val prediction = model.transform(input)

    /* //Rate
    val input = spark
      .readStream
      .format("rate")
      .option("rowPerSecond", 100)
      .option("numPartitions", 1)
      .load
  */

    // Socket
    /*val input = spark
      .readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", 9999)
      .option("includeTimestamp", true)
      .load
*/
    /* // CSV
    input.writeStream
      .format("csv")
      .option("path","src/main/resources/test")
      .outputMode("append")
      .option("checkpointLocation", checkpointLocation)
      .start()
      .awaitTermination()
*/
    // Start running the query that prints the running counts to the console
    // Console
    /*    val query = input.writeStream
      .outputMode("append")
      .queryName("datatable")
      .format("console")
      //.trigger(Trigger.ProcessingTime("2 seconds"))
      .trigger(Trigger.Once())
      .option("checkpointLocation", checkpointLocation)
      .start()
      .awaitTermination()
*/

    // Kafka
    val outputColumns: Array[String] = Array("sepal_length", "sepal_width", "petal_length", "petal_width", "prediction")
    val query = prediction
      .select(to_json(struct(outputColumns.map(col(_)): _*)).alias("value"))
      .writeStream
      .option("checkpointLocation", checkpointLocation)
      .outputMode("append")
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServers)
      .option("topic", topicOut)
      .start()
      .awaitTermination()

  }
}
