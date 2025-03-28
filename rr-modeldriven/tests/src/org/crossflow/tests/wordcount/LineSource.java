package org.crossflow.tests.wordcount;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class LineSource extends LineSourceBase {
	
	@Override
	public void produce() throws Exception {
		System.out.println(workflow.getInputDirectory());
		for (File file : workflow.getInputDirectory().listFiles()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					sendToLines(new Line(line));
				}
			}
		}
	}

}