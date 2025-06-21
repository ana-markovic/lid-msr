# Techrank Crossflow Baseline
This experiment implements the LID MSR workflow introduced in **Section 4.1 of the thesis**, which searches for the co-occurrence of model-driven engineering technologies in open-source GitHub repositories. The implementation uses the Crossflow framework to execute the the described workflow in a controlled, local environment.

## 📘 Related Thesis Sections

- **Section 4.1** – MSR workflow definition
- **Section 4.3** – Experimental setup and input preparation
- **Section 4.5** – Results and analysis

📎 *Reference: `add_link_when_published`*

## ✅ Prerequisites

- Java (JDK 11+)
- Docker
- Maven

## 🚀 How to Run
### 1.Setup the ActiveMQ

```bash
cd baseline-modeldriven
mvn clean
mvn compile
```

### 2. Compile the Crossflow Project

# ActiveMQ required to run the workflow
```
docker run -p 61616:61616 --name=activemq -p 8161:8161 -p 1099:1099 -d antonw/activemq-jmx
```

### 3. Run Experiments via Tests
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
