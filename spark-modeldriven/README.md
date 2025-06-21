# Techrank Spark
This experiment implements the LID MSR workflow introduced in **Section 4.1 of the thesis**, which searches for the co-occurrence of model-driven engineering technologies in open-source GitHub repositories. The implementation uses Apache Spark to execute the the described workflow in a controlled, local environment.

## 📘 Related Thesis Sections

- **Section 4.1** – MSR workflow definition
- **Section 4.3** – Experimental setup and input preparation
- **Section 4.5** – Results and analysis

📎 *Reference: `add_link_when_published`*

## ✅ Prerequisites

- Apache Spark (e.g. 3.5.4)
- Java (JDK 8+)
- Gradle

## 🚀 How to Run
```bash
# Set Spark home path
SPARK_HOME=~/Downloads/spark-3.5.4-bin-hadoop3 

# Build the project
./gradlew clean build

# Run the Spark job locally with 5 workers (threads)
$SPARK_HOME/bin/spark-submit --class markovic.ana.techrank.TechRank --master 'local[5]' app/build/libs/app-spark.jar
```
