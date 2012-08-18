import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.map.MultiValueMap;

/**
 * @author Roberto Trifiletti
 *
 */
public class CircuitConverter implements Runnable {

	private Charset charset;
	private File outputFile;
	private boolean timed;

	private MultiValueMap leftMap;
	private MultiValueMap rightMap;
	private HashMap<Integer, Gate> outputMap;

	private FairplayCircuitParser circuitParser;

	/**
	 * @param circuitFile
	 * @param outputFile
	 */
	public CircuitConverter(File circuitFile, File outputFile, boolean timed) {
		this.outputFile = outputFile;
		this.timed = timed;
		charset = Charset.defaultCharset();
		circuitParser = new FairplayCircuitParser(circuitFile, charset);

		leftMap = new MultiValueMap();
		rightMap = new MultiValueMap();
		outputMap = new HashMap<Integer, Gate>();
	}

	@Override
	public void run() {
		long startTime = System.currentTimeMillis();

		List<Gate> gates = circuitParser.getParsedGates();
		initMaps(gates);
		removeBlankWires(gates);
		
		List<List<Gate>> layersOfGates = getLayersOfGates(gates);

		writeOutput(layersOfGates);

		if(timed == true){
			System.out.println("The converting took: " +
					((System.currentTimeMillis() - startTime) / 1000) + " sec");
		}

	}

	/**
	 * @param gates
	 * @return A lists of lists where each list represents a layer of gates in
	 * the converted circuit
	 */
	@SuppressWarnings("unchecked")
	public List<List<Gate>> getLayersOfGates(List<Gate> gates) {
		List<List<Gate>> layersOfGates = new ArrayList<List<Gate>>();

		int totalNumberOfInputs = circuitParser.getTotalNumberOfInputs();
		/*
		 * Loop to run through each list in our MultiMap, first runs through all
		 * gates with left input 0, 1, 2, ..., 255.
		 * For each of these "input" dependant gates, we visit them recursively
		 * and set a timestamp on each of these.
		 */
		for(int i = 0; i < totalNumberOfInputs; i++){
			Collection<Gate> leftList = leftMap.getCollection(i);
			if(leftList == null){
				continue;
			}
			for(Gate g: leftList){
				visitGate(g, 0, layersOfGates);
			}
		}

		/*
		 * Now that we've visited all gates which depends on a left input, we
		 * do the same for the right input and recursively visit them again.
		 * When we visit a gate which has already been visited we set a
		 * timestamp again to be the max og the current time and the timestamp
		 * of the gate. This value determines which layer the gate is to be
		 * placed in. 
		 */
		for(int i = 0; i < totalNumberOfInputs; i++){
			Collection<Gate> rightList = rightMap.getCollection(i);
			if(rightList == null){
				continue;
			}
			for(Gate g: rightList){
				layersOfGates = visitGate(g, 0, layersOfGates);
			}
		}
		return layersOfGates;
	}

	/*
	 * We fill up our auxiliary maps which will help us find gates which are
	 * depending on a given gate. These Maps are MultiValued, so if two
	 * elements have the same key a list is created to hold each value associated to this
	 * key.
	 */
	private void initMaps(List<Gate> gates){
		for(Gate g: gates){
			leftMap.put(g.getLeftWireIndex(), g);
			rightMap.put(g.getRightWireIndex(), g);
			outputMap.put(g.getOutputWireIndex(), g);
		}
	}
	
	/**
	 * Assumes the inputFile is not sorted by outputWire. If this is the case,
	 * this code can be greatly optimized.
	 * @param gates
	 */
	private void removeBlankWires(List<Gate> gates){
		
		// false means blank
		boolean[] blankWires = circuitParser.getBlankWires();
		int numberOfWires = circuitParser.getNumberOfWires();

		// Runs from top to bottom, decrementing the appropriate wires
		for(int i =  numberOfWires - 1; i >= 0; i--){
			boolean b = blankWires[i];
			if(!b){
				for(int j = i; j < numberOfWires; j++){
					Gate g = outputMap.get(j);
					if(g != null){
						int outputIndex = g.getOutputWireIndex();
						g = gates.get(gates.indexOf(g));
						g.setOutputWireIndex(outputIndex - 1);
					}

				}
			}
		}
	}

	/**
	 * @param g
	 * @param time
	 * @param layersOfGates
	 * @return A list of lists representing each layer in the converted circuit
	 */
	private List<List<Gate>> visitGate(Gate g, int time, List<List<Gate>> layersOfGates) {
		g.decCounter();
		g.setTime(time);
		if (g.getCounter() == 0){
			g.setTime(time);
			addToSublist(g, layersOfGates);
			for(Gate outputGate: getOutputGates(g)){
				visitGate(outputGate, g.getTime() + 1, layersOfGates);
			}
		}
		return layersOfGates;
	}

	/**
	 * @param g
	 * @return A list of all gates depending directly on the given gate
	 */
	@SuppressWarnings("unchecked")
	private List<Gate> getOutputGates(Gate g){
		List<Gate> res = new ArrayList<Gate>();
		int inputIndex = g.getOutputWireIndex();
		Collection<Gate> leftList = leftMap.getCollection(inputIndex);
		Collection<Gate> rightList = rightMap.getCollection(inputIndex);

		if (leftList != null){
			res.addAll(leftList);
		}

		if (rightList != null){
			res.addAll(rightList);
		}

		return res;
	}

	/**
	 * @param g
	 * @param gates
	 * @return A List of lists where the given gate has been added to the
	 * correct sublists depending on it's timestamp
	 */
	private List<List<Gate>> addToSublist(Gate g, List<List<Gate>> layersOfGates){

		while(layersOfGates.size() <= g.getTime()){
			layersOfGates.add(new ArrayList<Gate>());
		}

		List<Gate> layer = layersOfGates.get(g.getTime());
		layer.add(g);

		return layersOfGates;
	}

	/**
	 * @param layersOfGates
	 * Writes the given lists of lists to a file
	 */
	private void writeOutput(List<List<Gate>> layersOfGates) {
		BufferedWriter fbw = null;
		try {
			fbw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), charset));
			int[] intHeaders = circuitParser.getOutputHeader(layersOfGates);
			String header = "";

			for (int i = 0; i < intHeaders.length; i++){
				header += (intHeaders[i] + "");
				if (i != intHeaders.length - 1){
					header += " ";
				}
			}
			fbw.write(header);
			fbw.newLine();

			/*
			 * Write the gates the the file, one layer at a time
			 */
			for(List<Gate> l: layersOfGates){
				// Write the size of the current layer
				fbw.write("*" + l.size()); 
				fbw.newLine();

				// Write the gates in this layer
				for(Gate g: l){
					String gateString = layersOfGates.indexOf(l) + " " + g.toString();
					fbw.write(gateString);
					fbw.newLine();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally { 
			try {
				fbw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public List<Gate> getParsedGates(){
		return circuitParser.getParsedGates();
	}

	public int[] getOutputHeader(List<List<Gate>>layersOfGates){
		return circuitParser.getOutputHeader(layersOfGates);
	}
}
