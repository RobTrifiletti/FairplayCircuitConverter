public interface Gate {

	public int getLeftWireIndex();

	public int getRightWireIndex();

	public int getOutputWireIndex();

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