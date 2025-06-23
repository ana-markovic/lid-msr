# Techrank - Bidding and Self-Correcting Workers
This project implements the historic source code analysis MSR workflow described in **Section 7.3.1** of the thesis using the Crossflow platform extended with a **Bidding Scheduler and self-correcting workers**. Each worker maintains its own linear estimator of job execution time and updates it dynamically based on observed job outcomes using online linear regression.

## 📘 Related Thesis Sections

- **Section 7.3.1** – MSR workflow definition for feedback-based estimator
- **Section 7.3.2** – Experimental setup, job creation and worker parameters
- **Section 7.3.3** – Result analysis and impact of online model refinement

📎 *Reference: `add_link_when_published`*

## ✅ Prerequisites
- Java 17
- Maven
- Docker

## 🚀 How to Run
### 1. Build executables
Make sure you have your `JAVA_HOME` environment variable set to your JDK 17 installation directory.

```bash
# Move to the project root if you haven't already
cd bidding-cf-linreg

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
cd workflow

# ActiveMQ required to run the workflow
docker run -p 61616:61616 --name=activemq -p 8161:8161 -p 1099:1099 -d antonw/activemq-jmx

# Set the Github auth environment variables
export GITHUB_TOKEN=<your_github_token>
export GITHUB_USERNAME=<your_github_user>
# Set the directory to download the repositories to
export STORAGE_DIR=<path_to_storage_directory>

#Select the correction factor (LIN_REG, NO_CF) in bidding-cf/workflow/start_worker.sh 

# Run the workflow
source start_master.sh && source start_worker.sh
```
### 3. Output
After execution, metric files will be written directly to the **workflow directory**.
