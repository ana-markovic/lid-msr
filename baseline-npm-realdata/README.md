# Techrank NPM Real Data - Crossflow Baseline
This project implements a real-world version of the MSR workflow, focusing on the co-occurrence of popular NPM libraries within GitHub repositories. It uses the Crossflow framework in its baseline configuration and is prepared to be run on AWS or similar cluster (can be used locally too)
.
## 📘 Related Thesis Sections

- **Section 6.4.1** – MSR workflow description for identifying which NPM libraries often appear together
- **Section 6.4.3** – Performance and output analysis
  
📎 *Reference: `add_link_when_published`*

## ✅ Prerequisites
- Java 17
- Maven
- Docker


## 🚀 How to Run
### 1. Build executables
Make sure you have your `JAVA_HOME` environment variable set to your JDK 17 installation directory.

```bash
# Move to the project root
cd techrank-npm-real-data

# Run the build script
./build.sh
```

### 2. Run the executables
You will need your GitHub token and username to run the workflow.
Follow these instructions to get the token:
[Creating a personal access token](https://docs.github.com/en/github/authenticating-to-github/creating-a-personal-access-token)

```bash
# Move to the workflow directory 
# (omit the root directory if you are already there)
cd /workflow

# ActiveMQ required to run the workflow
docker run -p 61616:61616 --name=activemq -p 8161:8161 -p 1099:1099 -d antonw/activemq-jmx

# Set the Github auth environment variables
export GITHUB_TOKEN=<your_github_token>
export GITHUB_USERNAME=<your_github_user>
# Set the directory to download the repositories to
export STORAGE_DIR=<path_to_storage_directory>

# Run the workflow
source start_master.sh && source start_worker.sh
```
### 3. Output
After execution, metric files will be written directly to the **workflow directory**.
