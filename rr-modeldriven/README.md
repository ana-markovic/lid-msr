# Techrank RR

## Description

## Prerequisites

Make sure you have `redis` and `activemq with jmx` up and running.

```bash
docker rm -f redis-server && \
docker run --name redis-server -p 6379:6379 -d redis && \
docker rm -f activemq && \
docker run -p 61616:61616 --name=activemq -p 8161:8161 -p 1099:1099 -d antonw/activemq-jmx
```