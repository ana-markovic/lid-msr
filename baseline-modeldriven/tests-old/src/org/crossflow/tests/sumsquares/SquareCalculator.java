package org.crossflow.tests.sumsquares;


public class SquareCalculator extends SquareCalculatorBase {
	
	@Override
	public Square consumeNumbers(Number number) throws Exception {
		long start = System.currentTimeMillis();

		Square squareInst = new Square();
		squareInst.setA(number.getA());
		squareInst.setSquare((int) Math.pow(number.getA(), 2));

		this.getWorkflow().addWorkTime(System.currentTimeMillis()-start);
		return squareInst;
	
	}


}
