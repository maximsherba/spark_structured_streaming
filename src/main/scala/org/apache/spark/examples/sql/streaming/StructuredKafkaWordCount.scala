/*
Построить модель классификации Ирисов Фишера и сохранить её.
Описание набора данных: https://ru.wikipedia.org/wiki/%D0%98%D1%80%D0%B8%D1%81%D1%8B_%D0%A4%D0%B8%D1%88%D0%B5%D1%80%D0%B0
Набор данных в формате CSV: https://www.kaggle.com/arshid/iris-flower-dataset
Набор данных в формате LIBSVM: https://github.com/apache/spark/blob/v3.2.3/data/mllib/iris_libsvm.txt
Должен быть предоставлен код построения модели (ноутбук или программа)
Разработать приложение, которое читает из одной темы Kafka (например, "input") CSV-записи с четырми признаками ирисов, и возвращает в другую тему (например, "predictition") CSV-записи с теми же признаками и классом ириса.
 */
package org.apache.spark.examples.sql.streaming

import org.apache.spark.SparkConf

import java.util.UUID
import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.functions.{col, struct, to_json}
//import org.apache.spark.sql.streaming.Trigger

//1 Двухфазный коммит - сначала в целевой кафке, затем в источнике
//2 Как реализовать корректный выход из приложения (например, надо временно остановить кластер для обслуживания)
//3 Что не хватает для продуктивного решения

object StructuredKafkaWordCount {
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
      .option("startingOffsets","earliest")
      //.option("enable.auto.commit","false")
      .option(subscribeType, topicIn)
      .load()
      .selectExpr("CAST(value AS STRING)")
      .as[String].map(_.split(","))
      .map(Model(_))

    // Применяем модель к входным данным
    val model = PipelineModel.load(pathToModel) //"/spark/Scala/IrisModel"
    val prediction = model.transform(input)

    //input.show()
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

