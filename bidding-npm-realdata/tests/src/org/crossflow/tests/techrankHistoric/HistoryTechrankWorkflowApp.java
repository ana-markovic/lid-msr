package org.crossflow.tests.techrankHistoric;

import org.crossflow.runtime.Mode;

public class HistoryTechrankWorkflowApp {

	public static void main(String[] args) throws Exception {
		
		HistoryTechrankWorkflow master = new HistoryTechrankWorkflow(Mode.MASTER);
		master.createBroker(true);
		master.setMaster("localhost");
		
		//master.setParallelization(4);
		
		//master.setInputDirectory(new File("experiment/in"));
		//master.setOutputDirectory(new File("experiment/out"));
		
		master.setInstanceId("Example HistoryTechrankWorkflow Instance");
		master.setName("HistoryTechrankWorkflow");
		
		master.run();
		
		master.awaitTermination();
		
		//System.out.println("Done");
		
	}
	
}
