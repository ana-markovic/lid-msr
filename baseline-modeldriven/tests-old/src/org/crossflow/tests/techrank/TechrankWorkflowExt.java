package org.crossflow.tests.techrank;

import org.crossflow.runtime.Mode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class TechrankWorkflowExt extends TechrankWorkflow {

    public long net_bytesPerSecond = 1 << 21; // 2MB / s
    public long io_bytesPerSecond = 1 << 27; // 128 MB / s
    public Set<String> downloaded = new HashSet<>();

    public String repoInputFile;

    private int cacheMisses = 0;

    public long workTimeMillis;
    private BigInteger bytesLoaded = BigInteger.ZERO;

    public TechrankWorkflowExt(Mode m) {
        super(m);
    }

    @Override
    public synchronized void terminate() {
        if (getResultsSink() != null) {
            this.getResultsSink().printMatrix();
        }
        saveDownloadedRepositories();
        if (!isMaster()) printWorkerStatistics();
        if (isMaster()) {
            saveMetricsCsv();
        }
        super.terminate();
    }

    public void loadDownloadedRepositories() {
        if (isMaster()) {
            return;
        }

        try {
            File file = new File(String.format("%s_downloaded_repositories.txt", getName()));
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    downloaded.add(line);
                }
                scanner.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveDownloadedRepositories() {
        try {
            File file = new File(String.format("%s_downloaded_repositories.txt", getName()));
            FileWriter writer = new FileWriter(file);
            for (String path : downloaded) {
                writer.write(path + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setNet_bytesPerSecond(long bytesPerSecond) {
        this.net_bytesPerSecond = bytesPerSecond;
    }

    public void setIo_bytesPerSecond(long io_bytesPerSecond) {
        this.io_bytesPerSecond = io_bytesPerSecond;
    }

    public void addLocalWorkTime(long milis) {
        workTimeMillis += milis;
    }

    public void addCacheData(int numMisses, long dataLoaded) {
        cacheMisses += numMisses;
        bytesLoaded = bytesLoaded.add(BigInteger.valueOf(dataLoaded));
    }

    public void printWorkerStatistics() {
        String fileName = String.format("statistics-i%s-%s-%s-%s-%s.txt", iteration, algorithm, jobConf, workerConf, getName());
        try {
            Files.write(Paths.get(fileName), getWorkerStatistics().getBytes());
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getWorkerStatistics() {
        return String.format("workername: %s, cachemisses: %s, dataloaded: %s, worktime: %s", getName(), cacheMisses, bytesLoaded, workTimeMillis);
    }

    public void printMasterStatistics(String masterStatistics) {
        String fileName = String.format("statistics-i%s-%s-%s-%s-master.txt", iteration, algorithm, jobConf, workerConf);
        try {
            Files.write(Paths.get(fileName), masterStatistics.getBytes());
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setupSpeed() {
        Scanner scanner;
        try {
            String speedLocation = "/home/ubuntu/";
            if (System.getenv().containsKey("SPEED_LOCATION")) {
                speedLocation = System.getenv("SPEED_LOCATION");
            }
            scanner = new Scanner(new File(speedLocation + getName() + "-speed.txt"));
            String netSpeedStr = scanner.nextLine();
            String ioSpeedStr = scanner.nextLine();
            setIo_bytesPerSecond(Long.parseLong(ioSpeedStr));
            setNet_bytesPerSecond(Long.parseLong(netSpeedStr));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public long getNetSpeed() {
        return SpeedUtils.getSpeed(net_bytesPerSecond);
    }

    public long getIOSpeed() {
        return SpeedUtils.getSpeed(io_bytesPerSecond);
    }

}
