# Techrank Spark

## Prerequisites
- Apache spark
- Gradle

## How to run
```bash
#Path to your downloaded spark folder
SPARK_HOME=~/Downloads/spark-3.5.4-bin-hadoop3 
./gradlew clean build
$SPARK_HOME/bin/spark-submit --class markovic.ana.techrank.TechRank --master 'local[5]' app/build/libs/app-spark.jar
```