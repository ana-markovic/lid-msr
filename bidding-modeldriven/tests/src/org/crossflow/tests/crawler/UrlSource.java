package org.crossflow.tests.crawler;

public class UrlSource extends UrlSourceBase {
	
	@Override
	public void produce() {
		sendToUrls(new Url("index.html"));
	}

}