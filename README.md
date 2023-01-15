# Spark Structured Streaming + Kafka 

Пример простого приложения Spark Structured Streaming, которое читает данные из Kafka, прогоняет их через ML модель и загружает в другой топик Kafka

## Запуск

* Запускаем Kafka + Spark в докере - docker-compose.yml
* Запускаем загрузку данных во входящий топик
sudo awk -F ',' 'NR > 1 { system("sleep 1"); print $1 "," $2 "," $3 "," $4 }' < spark/data/IRIS.csv | sudo docker exec -i kafka-broker kafka-console-producer --topic input --bootstrap-server kafka:9092
* Запускаем spark-submit с параметрами 
sudo docker exec -it spark-master spark-submit /spark/Scala/StructuredKafkaWordCount-assembly-1.0.jar spark://master:7077 /spark/Scala/IrisModel kafka:9092 subscribe input prediction