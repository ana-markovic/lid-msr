# Flink Techrank
This experiment implements the LID MSR workflow introduced in **Section 4.1 of the thesis**, which searches for the co-occurrence of model-driven engineering technologies in open-source GitHub repositories. The implementation uses Apache Flink to execute the the described workflow in a controlled, local environment.

## 📘 Related Thesis Sections

- **Section 4.1** – MSR workflow definition
- **Section 4.3** – Experimental setup and input preparation
- **Section 4.5** – Results and analysis

📎 *Reference: `add_link_when_published`*


## ✅ Prerequisites

- Apache Flink (e.g. 1.20.0)
- Java JDK 8+
- Maven

## 🚀 How to Run

```bash
# Build the Flink job
mvn clean package

# Define Flink path
FLINK_HOME="$HOME/Downloads/flink-1.20.0"

# Start cluster before running the job
$FLINK_HOME/bin/start-cluster.sh

# Run the job
$FLINK_HOME/bin/flink run target/flink-techrank-1.0.jar

# Stop cluster when done
$FLINK_HOME/bin/start-cluster.sh
```

## Check results

```bash
cat $FLINK_HOME/log/*.out```
