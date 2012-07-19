public interface Gate {

	public abstract int getLeftWireIndex();

	public abstract int getRightWireIndex();

	public abstract int getOutputWireIndex();

	public abstract int getCounter();

	public abstract void decCounter();

	public abstract int getTime();

	public abstract void setTime(int time);

	public abstract String getGate();

	public abstract String toString();

	public abstract boolean isXOR();

	public abstract void setGateNumber(int gateNumber);

	public abstract int getGateNumber();

}