package it.softsolutions.bestx.services.logutils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ApplicationMonitorTest {
	private ApplicationMonitor monitor = new ApplicationMonitor(); 
	
	@Test
	public void test() {
		try {
			ApplicationMonitor.onNewOrder("Connection 1");
			Thread.sleep(2000);
			ApplicationMonitor.onNewOrder("Connection 1");
			Thread.sleep(2000);
			ApplicationMonitor.onNewOrder("Connection 1");
			Thread.sleep(2000);
			ApplicationMonitor.onNewOrder("Connection 2");
			Thread.sleep(2000);
			ApplicationMonitor.onNewOrder("Connection 2");
		} catch (InterruptedException e) { }
		
		String output = monitor.buildInputThroughputStats(60);
		System.out.println(output);
		assertEquals("{Connection 1:3,Connection 2:2}", output);

		output = monitor.buildInputThroughputStats(5);
		assertEquals("{Connection 1:1,Connection 2:2}", output);

		output = monitor.buildInputThroughputStats(1);
		assertEquals("{Connection 1:0,Connection 2:1}", output);
	}
}
