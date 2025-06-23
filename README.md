# LID-MSR: Locality-Intensive Distributed Mining of Software Repositories

This repository contains the complete research artefact accompanying the thesis:

**"Locality-Aware Scheduling of Software Repository Mining Workflows in Heterogeneous Environments"**  
Author: Ana Markovic  
Institution: University of York  
Thesis Document: `add_link_when_published`

The work investigates job scheduling strategies for mining software repositories (MSR) at scale, with a focus on locality-intensive workflows and decentralised scheduling in environments where workers may possess varying physical characteristics.

---

## 📚 Thesis Structure and Mapping

Each subdirectory corresponds to one or more experiments described in the thesis. All experiments are reproducible and described in detail in their respective README files.

| Directory                    | Description                                                                        | Thesis Sections                        |
|-----------------------------|-------------------------------------------------------------------------------------|----------------------------------------|
| `spark-modeldriven/`        | Running MSR workflow using Apache Spark                                             | 4.1, 4.4, 4.5                          |
| `flink-modeldriven/`        | Running MSR workflow using Apache Flink                                             | 4.1, 4.4, 4.5                          |
| `baseline-modeldriven/`     | Running MSR workflow with Crossflow                                                 | 4.1, 4.4, 4.5                          |
| `rr-modeldriven/`           | Crossflow with the Resource-Registry Scheduler                                      | 4.1, 4.3, 5.2                          |
| `bidding-modeldriven/`      | Crossflow with the Bidding Scheduler                                                | 4.1, 4.3, 6.3                          |
| `baseline-npm-realdata/`    | Real-world MSR workflow on GitHub/NPM data (Crossflow baseline)                     | 6.4.1, 6.4.2                           |
| `bidding-npm-realdata/`     | Real-world MSR workflow on GitHub/NPM data (Crossflow with the Bidding Scheduler)   | 6.4.1, 6.4.2                           |
| `bidding-cf/`               | Historic repository analysis with the Bidding Scheduler and self-correcting workers | 7.3.1, 7.3.2, 7.3.3                    |

---

## 📦 What’s Included

- Workflow implementations for baseline selection (Spark, Flink, Crossflow)
- Synthetic and real-data experiment runners
- Input examples and configuration files
- Reproduction instructions for each experiment

---

## 🛠 Requirements

- Java 17
- Maven or Gradle (depending on the module)
- Apache Spark / Flink (for specific modules)
- Docker (for Redis and ActiveMQ, used by Crossflow)
- GitHub Personal Access Token (for real-data MSR experiments)

---

## 🧪 Reproducibility

Each submodule contains:
- A `README.md` with setup and execution instructions 
- References to the corresponding sections in the thesis

To reproduce thesis results, follow the specific instructions in each subfolder.

---

## 📜 Licence

This repository is provided as part of academic research. Please cite the thesis if using this artefact in your own work.

---
