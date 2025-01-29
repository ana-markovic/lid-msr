package org.crossflow.tests.techrankHistoric;


import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ResultsSink extends ResultsSinkBase {

	private final Map<String, Integer> commitWarnings = new HashMap<>();

	@Override
	public void consumeCommitAnalysisResults(CommitAnalysisResult result) throws Exception {
		long taskStart = System.currentTimeMillis();
//		commitWarnings.put(result.getCommitHash(), result.getWarningCount());
		String key = result.getRepositoryName() + "," + result.getCommitHash() + "," + result.commitNumber;
		commitWarnings.put(key, result.getWarningCount());
		getWorkflow().addWorkTime(System.currentTimeMillis() - taskStart);
	}


	public void printMatrix() {
		System.out.println("CommitHash,WarningCount");
        commitWarnings.forEach((commit, warningCount) -> System.out.println(commit + "," + warningCount));
	}

	public void printToCSV() {
		try (PrintWriter writer = new PrintWriter("results.csv", "UTF-8")) {
			writer.println("RepositoryName,CommitHash,CommitNumber,WarningCount");
			commitWarnings.forEach((commit, warningCount) -> writer.println(commit + "," + warningCount));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
