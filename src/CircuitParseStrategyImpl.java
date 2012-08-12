import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class CircuitParseStrategyImpl<E> implements CircuitParseStrategy<Gate> {

	private File circuitFile;
	private Charset charset;
	private int numberOfAliceInputs;
	private int numberOfBobInputs;
	private int numberOfNonXORGates;
	private int totalNumberOfInputs;

	public CircuitParseStrategyImpl(File circuitFile, Charset charset){

		this.circuitFile = circuitFile;
		this.charset = charset;
	}

	/**
	 * @return A list of gates in the given circuitFile
	 */
	public List<Gate> getParsedGates() {
		boolean counter = false;
		ArrayList<Gate> res = new ArrayList<Gate>();
		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(circuitFile), charset));
			String line = "";
			while((line = fbr.readLine()) != null) {
				if (line.isEmpty()){
					continue;
				}

				/*
				 * Ignore meta-data info, we don't need it
				 */
				if(line.matches("[0-9]* [0-9]*")){
					counter = true;
					continue;
				}

				/*
				 * Parse number of input bits
				 */
				if (counter == true){
					String[] split = line.split(" ");
					numberOfAliceInputs = Integer.parseInt(split[0]);
					numberOfBobInputs = Integer.parseInt(split[1]);
					totalNumberOfInputs = numberOfAliceInputs +
							numberOfBobInputs;
					counter = false;
					continue;
				}

				/*
				 * Parse each gate line and count numberOfNonXORGates
				 */
				Gate g = new GateConvert(line);
				if (!g.isXOR()){
					g.setGateNumber(numberOfNonXORGates);
					numberOfNonXORGates++;
				}
				res.add(g);
			}
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return res;
	}

	public int[] getHeader(List<List<Gate>> sortedGates){
		int numberOfTotalOutputs = totalNumberOfInputs/2;
		int numberOfWires = getNumberOfWires(sortedGates);
		int numberOfLayers = sortedGates.size();

		int maxLayerWidth = 0;

		/*
		 * We have to figure out the max layer size before writing to the file.
		 */
		for(List<Gate> l: sortedGates){
			maxLayerWidth = Math.max(maxLayerWidth, l.size());
		}

		/*
		 * Build and write the header of the output file
		 */
		int[] header = new int[]{
				totalNumberOfInputs,
				numberOfTotalOutputs,
				numberOfWires,
				numberOfLayers,
				maxLayerWidth,
				numberOfNonXORGates};
		return header;
	}

	public int getTotalNumberOfInputs(){
		return totalNumberOfInputs;
	}

	/**
	 * @param multiTimedGates
	 * @return
	 */
	private int getNumberOfWires(List<List<Gate>> multiTimedGates) {
		HashSet<Integer> hs = new HashSet<Integer>();
		for(List<Gate> l: multiTimedGates){
			for(Gate g: l){
				hs.add(g.getLeftWireIndex());
				hs.add(g.getRightWireIndex());
				hs.add(g.getOutputWireIndex());
			}
		}

		return hs.size();
	}

}
