package cnd;

import cnd.strategy.CndStrategy;
import cnd.strategy.PDPStrategy;

public class CombinationNDCLL {

	private CndStrategy strategy;
	
	public CombinationNDCLL() {
		
	}
	
	public String calculate() {
		return strategy.calculate();
	}
	
	public void setStrategy(CndStrategy strategy) {
		this.strategy = strategy;
	}
	
	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.out.println("Usage: <positive integer>");
			System.exit(1);
		}
		
		try {
			
			int length = Integer.parseInt(args[0]);
			CombinationNDCLL cnd = new CombinationNDCLL();
			cnd.setStrategy(new PDPStrategy(length));
			String result = cnd.calculate();
			System.out.println(result);
			
		} catch (NumberFormatException e) {
			System.out.println("<positive integer>‚É‚Í”¼Šp”š‚ğ“ü—Í‚µ‚Ä‚­‚¾‚³‚¢");
			System.out.println("Usage: <positive integer>");
			System.exit(-1);
		}

	}

}
