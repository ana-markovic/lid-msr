#!/bin/zsh

export WORKER_COUNT=5
export STORAGE_DIR=${STORAGE_DIR:-"$HOME/Desktop"}
export GITHUB_USERNAME=${GITHUB_USERNAME}
export GITHUB_TOKEN=${GITHUB_TOKEN}
#export CF_TYPE=LIN_REG
export CF_TYPE=NO_CF

fast_workers_count=1
slow_workers_count=2

# 1 .. NUM_WORKERS (keep in sync with $WORKER_COUNT)
for value in {1..5}
do
    if [ $value -le $fast_workers_count ]
    then
        # FAST settigns
        export WORKER_NAME="worker_fast_$value"
        export WORKER_TYPE="WORKER_FAST"
        export CPU_COST_PER_HOUR=0.001
        export STORAGE_COST_PER_HOUR=0.001
        export STORAGE_SIZE_GB=180
    elif [ $value -le $((fast_workers_count + slow_workers_count)) ]
    then
        # SLOW settings
        export WORKER_NAME="worker_slow_$value"
        export WORKER_TYPE="WORKER_SLOW"
        export CPU_COST_PER_HOUR=0.001
        export STORAGE_COST_PER_HOUR=0.001
        export STORAGE_SIZE_GB=180
    else
        # NORMAL settings
        export WORKER_NAME="worker_normal_$value"
        export WORKER_TYPE="WORKER_NORMAL"
        export CPU_COST_PER_HOUR=0.001
        export STORAGE_COST_PER_HOUR=0.001
        export STORAGE_SIZE_GB=180
    fi
    filename="$WORKER_NAME.log"
    java --add-opens java.base/java.time=ALL-UNNAMED -jar techrank-worker.jar > $filename 2>&1 &
done
