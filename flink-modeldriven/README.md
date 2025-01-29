# Flink Techrank

## Prerequisites

- Apache flink
- Maven

## How to run

```bash
mvn clean package

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
