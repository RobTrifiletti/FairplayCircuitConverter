import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections.map.MultiValueMap;

/**
 * @author Roberto
 *
 */
public class CircuitSorter implements Runnable {

	private Charset charset;
	private File circuitFile;
	private File outputFile;
	private static boolean timed;

	private int numberOfAliceInputs = 0;
	private int numberOfBobInputs = 0;
	private int totalNumberOfInputs = 0;
	private int numberOfNonXORGates = 0;

	private MultiValueMap leftMap;
	private MultiValueMap rightMap;

	/**
	 * @param circuitFile
	 * @param outputFile
	 */
	public CircuitSorter(File circuitFile, File outputFile) {
		this.circuitFile = circuitFile;
		this.outputFile = outputFile;
		charset = Charset.defaultCharset();
		leftMap = new MultiValueMap();
		rightMap = new MultiValueMap();
	}

	public void run() {
		long startTime = System.currentTimeMillis();

		List<Gate> gates = getParsedGates();
		List<List<Gate>> layersOfGates = getLayersOfGates(gates);
		writeOutput(layersOfGates);

		if(timed == true){
			System.out.println("The sorting took: " +
		((System.currentTimeMillis() - startTime) / 1000) + " sec");
		}

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
				Gate g = new GateSort(line);
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

	/**
	 * @param gates
	 * @return A lists of lists where each list represents a layer og gates in
	 * the sorted circuit
	 */
	public List<List<Gate>> getLayersOfGates(List<Gate> gates) {
		List<List<Gate>> layersOfGates = new ArrayList<List<Gate>>();

		/*
		 * We fill up our auxiliary maps which will help us find gates which are
		 * depending on a given gate. These Maps are MultiValued, so if two
		 * elements have the same key a list is created to hold each value associated to this
		 * key.
		 */
		for(Gate g: gates){
			leftMap.put(g.getLeftWireIndex(), g);
			rightMap.put(g.getRightWireIndex(), g);
		}

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

	/**
	 * @param g
	 * @param time
	 * @param layersOfGates
	 * @return A list of lists representing each layer in the sorted circuit
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
			String header = getHeader(layersOfGates);
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

	public String getHeader(List<List<Gate>> sortedGates){
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
		String header = totalNumberOfInputs + " " + numberOfTotalOutputs + " " +
				numberOfWires + " " + numberOfLayers + " " + maxLayerWidth +
				" " + numberOfNonXORGates;

		return header;
	}


	/**
	 * @param args
	 * Should be circuitFile.txt outputfilename.txt
	 * If no output filename is given, out.txt is chosen by default
	 * circuitFile.txt must exist on the file system, else error
	 * Optional: add a -t argument to get the running time of the sorting in
	 * standard out
	 */
	public static void main(String[] args) {
		if(args.length < 1){
			System.out.println("Incorrect number of arguments, please specify inputfile");
			return;
		}

		String circuitFilename = null;
		String outputFilename = null;

		/*
		 * Parse the arguments
		 */
		for(int param = 0; param < args.length; param++){
			if(args[param].equals("-t")){
				timed = true;
			}
			
			else if (circuitFilename == null) {
				circuitFilename = args[param];
			}

			else if (outputFilename == null) {
				outputFilename = args[param];
			}

			else System.out.println("Unparsed: " + args[param]); 
		}
		if(outputFilename == null) {
			outputFilename = "data/out.txt";
		}
		File circuitFile = new File(circuitFilename);

		File outputFile = new File(outputFilename);

		if (!circuitFile.exists()){
			System.out.println("Inputfile: " + circuitFile.getName() + " not found");
			return;
		}

		CircuitSorter sorter = new CircuitSorter(circuitFile, outputFile);
		sorter.run();
	}
}
