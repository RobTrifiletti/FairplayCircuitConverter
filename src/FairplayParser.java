import java.util.List;


public interface FairplayParser {
	
	public List<Gate> getParsedGates();
	
	public int[] getHeader(List<List<Gate>> sortedGates);
	
	public int getTotalNumberOfInputs();
}
