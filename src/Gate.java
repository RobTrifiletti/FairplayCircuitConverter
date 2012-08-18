public interface Gate {

	public int getLeftWireIndex();
	
	public void setLeftWireIndex(int index);

	public int getRightWireIndex();
	
	public void setRightWireIndex(int index);

	public int getOutputWireIndex();
	
	public void setOutputWireIndex(int index);

	public int getCounter();

	public void decCounter();

	public int getTime();

	public void setTime(int time);

	public String getGate();

	public String toString();

	public boolean isXOR();

	public void setGateNumber(int gateNumber);

	public int getGateNumber();

}