package org.crossflow.runtime.utils;

import org.crossflow.runtime.Job;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class KVSRepository {

    private final String JOBS_KEY_PREFIX = "jobs:";
    private final String WORKER_KEY_PREFIX = "work:";

    private JedisPool pool;

    public KVSRepository(String hostname, int port) {
        pool = new JedisPool(hostname, port);
    }

    public KVSRepository() {
        pool = new JedisPool();
    }

    public Set<String> getWorkerIds(String jobId) {
        try (Jedis jedis = pool.getResource()) {
            Set<String> smembers = jedis.smembers(JOBS_KEY_PREFIX + jobId);
            return smembers;
        }
    }

    public void setWorkerId(String jobId, String workerId) {
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(JOBS_KEY_PREFIX + jobId, workerId);
        }
    }

    public List<JobInfo> getJobsByWorker(String workerId) {
        if (workerId == null) {
            return Collections.EMPTY_LIST;
        }
        try (Jedis jedis = pool.getResource()) {
            return jedis.lrange(WORKER_KEY_PREFIX + workerId, 0, -1)
                    .stream()
                    .map(JobInfo::fromString)
                    .collect(toList());
        }
    }

    public List<JobInfo> getAssignedJobsToWorker(String workerId) {
        if (workerId == null) {
            return Collections.EMPTY_LIST;
        }
        try (Jedis jedis = pool.getResource()) {
            return jedis.lrange(WORKER_KEY_PREFIX + workerId, 0, -1)
                    .stream()
                    .map(JobInfo::fromString)
                    .filter(jobInfo -> jobInfo.getStatus().equals("ISSUED"))
                    .collect(toList());
        }
    }

    public void addJobInfo(JobInfo jobInfo, String workerId) {
        try (Jedis jedis = pool.getResource()) {
            List<JobInfo> jobsByWorker = getJobsByWorker(workerId);
            jedis.rpush(WORKER_KEY_PREFIX + workerId, jobInfo.toJson());
        }
    }

    public Optional<JobInfo> getJobInfoByHash(String workerId, String jobHash) {
        List<JobInfo> jobsByWorker = getJobsByWorker(workerId);
        return jobsByWorker.stream()
                .filter(jobInfo -> jobInfo.getJobHash().equals(jobHash))
                .findAny();
    }

    public boolean setJobStatus(String workerId, String jobHash, String status) {
        try (Jedis jedis = pool.getResource()) {
            List<JobInfo> jobsByWorker = getJobsByWorker(workerId);
            for (int i = 0; i < jobsByWorker.size(); i++) {
                JobInfo jobInfo = jobsByWorker.get(i);
                if (jobInfo.getJobHash().equals(jobHash)) {
                    jobInfo.setStatus(status);
                    jedis.lset(WORKER_KEY_PREFIX + workerId, i, jobInfo.toJson());
                    return true;
                }
            }
            return false;
        }
    }

    public void issueJob(Job job, String senderId, String destinationId) {
        JobInfo jobInfo = new JobInfo(job.getJobHash(), senderId, destinationId, job.getCost(), "ISSUED");
        addJobInfo(jobInfo, destinationId);
    }

    public void finishJob(Job job) {
        setJobStatus(job.getDesignatedWorkerId(), job.getJobHash(), "FINISHED");
    }

    public String getLeastBusyWorkerOf(Set<String> workerIds) {
        return workerIds.stream()
                .collect(toMap(
                        workerId -> workerId,
                        this::getAssignedJobsToWorker
                ))
                .entrySet()
                .stream()
                .min(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public boolean workersTooBusy(Set<String> workerIds) {
        return workerIds.stream()
                .collect(toMap(
                        workerId -> workerId,
                        this::getAssignedJobsToWorker
                ))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() < 4)
                .count() < 1;

    }
}