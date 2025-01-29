package org.crossflow.tests.terasort;


import java.io.File;

public class SortedFilesDispatcher extends SortedFilesDispatcherBase {

	public static final int TOTAL_LINES = 10_000_000;

	private SortedFilePath savedFile;
	private final String HDFSOutputFolder = "hdfs://localhost:9000/sorted_files";

	@Override
	public void consumeSortedFilePaths(SortedFilePath sortedFilePath) throws Exception {
		long startTime = System.currentTimeMillis();
		if (savedFile == null) {
			if (sortedFilePath.lineCount == TOTAL_LINES) {
				TerasortResult terasortResult = new TerasortResult();
				terasortResult.setSortedOutputFile(sortedFilePath);
				sendToTerasortResults( terasortResult);
				getWorkflow().addWorkTime(System.currentTimeMillis() - startTime);
				return;
			}
			savedFile = sortedFilePath;
		} else {
			SortedFilePair sortedFilePair = new SortedFilePair(savedFile, sortedFilePath);
			saveJobHash(sortedFilePair);
			sendToFilesForMerge(sortedFilePair);
			savedFile = null;
			getWorkflow().addWorkTime(System.currentTimeMillis() - startTime);
		}
	}

	private void saveJobHash(SortedFilePair filePair) {
		TerasortWorkflowExt extendedWorkflow = (TerasortWorkflowExt) getWorkflow();
		JobMetadata jobMetadata1 = new JobMetadata(
				filePair.firstSortedFile.getJobId(),
				HDFSOutputFolder + File.separator + filePair.firstSortedFile.fileName
		);
		JobMetadata jobMetadata2 = new JobMetadata(
				filePair.secondSortedFile.getJobId(),
				HDFSOutputFolder + File.separator + filePair.secondSortedFile.fileName
		);
	}
}
