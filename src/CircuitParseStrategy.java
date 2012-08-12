import java.util.List;


public interface CircuitParseStrategy<E> {
	
	public List<E> getParsedGates();
	public int[] getHeader(List<List<E>> sortedGates);
	public int getTotalNumberOfInputs();
}
