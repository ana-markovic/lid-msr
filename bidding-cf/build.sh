#!/bin/zsh

echo "Building worker..."
mvn clean package -DworkerType=worker -DmainClassName=HistoryTechrankWorkflowWorkerApp
echo "Success! Moving techrank-worker.jar to workflow directory"
mv tests/target/techrank-worker.jar workflow/

echo "Building master..."
mvn clean package -DworkerType=master -DmainClassName=HistoryTechrankWorkflowMasterApp
echo "Success! Moving techrank-master.jar to workflow directory"
mv tests/target/techrank-master.jar workflow/