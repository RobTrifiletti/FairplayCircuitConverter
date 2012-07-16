
public class GateOld {
	
	private int leftWire;
	private int rightWire;
	//private int outputWire;
	private String gateString;
	private GateOld leftGate;
	private GateOld rightGate;
	
	public GateOld(GateOld leftGate, GateOld rightGate, String gateString){
		this.leftGate = leftGate;
		this.rightGate = rightGate;
		this.gateString = gateString;
	}
	
	public GateOld(String s){
		String[] split = s.split(" ");
		this.leftWire = Integer.parseInt(split[2]);
		this.rightWire = Integer.parseInt(split[3]);
//		this.outputWire = Integer.parseInt(split[4]);
		this.gateString = s;
	}

	public int getLeftWire() {
		return leftWire;
	}

	public int getRightWire() {
		return rightWire;
	}
	
//	public int getOutputWire(){
//		return outputWire;
//	}
	
	public String getGateString(){
		return gateString;
	}

}
