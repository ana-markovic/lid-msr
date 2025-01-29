package org.crossflow.tests.terasort;


import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;

import java.io.*;

public class Sorter extends CommitmentSorterBase {
    private FileSystem fileSystem = HadoopConfiguration.getFileSystem();

    private String workerName = "worker";

    String inputFilePrefix = "unsorted";

    private final String HDFSInputFolder = "hdfs://localhost:9000/unsorted_files";
    private final String HDFSOutputFolder = "hdfs://localhost:9000/sorted_files";


    private final String sortedFileFormat = "sorted_%s";

    @Override
    public SortedFilePath consumeFilesForSort(UnsortedFilePath unsortedFilePath) throws Exception {
        long startTime = System.currentTimeMillis();

        InputStream fileInputStream = getFileInputStream(unsortedFilePath);

        String sortedFileName = getSortedFileName(unsortedFilePath);
        Path hadoopFile = new Path(HDFSOutputFolder, sortedFileName);

        FSDataOutputStream currentFile = fileSystem.create(hadoopFile);

        BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
        reader.lines().sorted().forEach(line -> {
            try {
                currentFile.writeBytes(line + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        reader.close();
        currentFile.close();

        SortedFilePath sortedFilePathInst = new SortedFilePath();
        sortedFilePathInst.setFileName(sortedFileName);
        sortedFilePathInst.setLineCount(unsortedFilePath.lineCount);

        long totalTime = System.currentTimeMillis() - startTime;
        getWorkflow().addWorkTime(totalTime);
        return sortedFilePathInst;
    }

    private String getSortedFileName(UnsortedFilePath unsortedFilePath) {
        String filename = unsortedFilePath.fileName.substring(
                unsortedFilePath.fileName.indexOf(inputFilePrefix) + inputFilePrefix.length()
        );
        return String.format(sortedFileFormat, filename);
    }

    private InputStream getFileInputStream(UnsortedFilePath unsortedFilePath) {
        try {
            Path hadoopFile = new Path(HDFSInputFolder, unsortedFilePath.fileName);
            return fileSystem.open(hadoopFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
