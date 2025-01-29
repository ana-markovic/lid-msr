package org.crossflow.tests.sumsquares;

import java.io.BufferedReader;
import java.io.FileReader;

public class SumSquareSource extends SumSquareSourceBase {

	@Override
	public void produce() throws Exception {
		long start = System.currentTimeMillis();

		BufferedReader parser = new BufferedReader(new FileReader(workflow.getInputDirectory() +  "/input.txt"));

		String line;
		while ((line = parser.readLine())!=null){
			Number number = new Number();
			number.setA(Integer.parseInt(line));
			sendToNumbers(number);
		}
		this.getWorkflow().addWorkTime(System.currentTimeMillis()-start);

	}
}
