# Techrank - Crossflow with the Resource-Registry Scheduler
This project implements the MSR workflow seeking the co-occurences of different model-driven technologies in GitHub repositories (as described in **Section 4.1 of the thesis**) using the Crossflow framework extended with the **Resource-Registry** scheduler to execute the the described workflow in a controlled, local environment.

## 📘 Related Thesis Sections

- **Section 4.1** – MSR workflow definition
- **Section 4.3** – Experimental setup and input preparation
- **Section 5.2** – Evaluation results for Resource-Registry-enabled execution

📎 *Reference: `add_link_when_published`*

## ✅ Prerequisites

- Java (JDK 11+)
- Docker
- Maven

## 🚀 How to Run
### 1. Setup the ActiveMQ and Redis
Make sure you have `redis` and `activemq with jmx` up and running.

```bash
docker rm -f redis-server && \
docker run --name redis-server -p 6379:6379 -d redis && \
docker rm -f activemq && \
docker run -p 61616:61616 --name=activemq -p 8161:8161 -p 1099:1099 -d antonw/activemq-jmx
```
### 2. Compile the Crossflow Project
```
cd rr-modeldriven
mvn clean
mvn compile
```
### 3.  Run the Experiments via Test Methods
Experiments are located in:
```org.crossflow.tests.techrank.TechrankMetricsTests```


Each method in this class corresponds to a unique combination of job and worker configurations. You may run a specific test using your IDE or with Maven test commands if configured.

Example:
```
TechrankMetricsTests.testConfOneSlow_allDiff100_10_10();
TechrankMetricsTests.testConfFastSlow_allDiff40_40_40();
...
```
### 4. Output
After execution, metric files will be written directly to the **root project directory**.

