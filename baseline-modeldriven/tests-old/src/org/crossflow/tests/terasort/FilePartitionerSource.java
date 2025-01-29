package org.crossflow.tests.terasort;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Scanner;

public class FilePartitionerSource extends FilePartitionerSourceBase {

    private final String HDFSInputFolder = "hdfs://localhost:9000/tera_source";
    private final String HDFSOutputFolder = "hdfs://localhost:9000/unsorted_files";

    private final String tbFileName = "dump.txt";
    private final int blockLineCount = 250_000;

    private int currentLine = 0;
    private int filesProduced = 0;

    private String fileNameFormat = "unsorted%05d";
    private FileSystem fileSystem = HadoopConfiguration.getFileSystem();
    private FSDataOutputStream currentFile;


    @Override
    public void produce() throws Exception {
        long startTime = System.currentTimeMillis();
        Path tbFilePath = new Path(HDFSInputFolder, tbFileName);
        FSDataInputStream in = fileSystem.open(tbFilePath);
        Scanner reader = new Scanner(new InputStreamReader(in));

        while (reader.hasNext()) {
            if (currentLine % blockLineCount == 0) {
                if (currentFile != null) {
                    currentFile.close();

                    UnsortedFilePath unsortedFilePath = new UnsortedFilePath(
                            String.format(fileNameFormat, filesProduced),
                            blockLineCount
                    );
                    sendToFilesForSort(unsortedFilePath);
                }
                filesProduced++;
                String newFileName = String.format(fileNameFormat, filesProduced);
                Path newPath = new Path(HDFSOutputFolder, newFileName);
                currentFile = fileSystem.create(newPath);
            }

            currentLine++;
            currentFile.writeBytes(reader.nextLine() + "\n");
        }

        if (currentFile != null) {
            currentFile.close();
        }

        UnsortedFilePath unsortedFilePath = new UnsortedFilePath(
                String.format(fileNameFormat, filesProduced),
                blockLineCount
        );
        saveJobHash(unsortedFilePath);
        sendToFilesForSort(unsortedFilePath);
        getWorkflow().addWorkTime(System.currentTimeMillis() - startTime);
    }

    private void saveJobHash(UnsortedFilePath unsortedFilePath) {
        TerasortWorkflowExt extendedWorkflow = (TerasortWorkflowExt) getWorkflow();
        JobMetadata jobMetadata = new JobMetadata(
                unsortedFilePath.getHash(),
                HDFSOutputFolder + File.separator + unsortedFilePath.fileName
        );
    }
}
