#!/bin/zsh

export WORKER_COUNT=5
export WORKER_NAME=master
export GITHUB_USERNAME=${GITHUB_USERNAME}
export GITHUB_TOKEN=${GITHUB_TOKEN}

java --add-opens java.base/java.time=ALL-UNNAMED -jar techrank-master.jar > master.log 2>&1 &
