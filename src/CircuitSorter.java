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
	private File inputFile;
	private File outputFile;

	private int numberOfAliceInputs = 0;
	private int numberOfBobInputs = 0;
	private int numberOfNonXORGates = 0;

	private MultiValueMap leftMap;
	private MultiValueMap rightMap;

	/**
	 * @param inputfileName
	 * @param outputfileName
	 */
	public CircuitSorter(File inputFile, File outputFile) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		charset = Charset.defaultCharset();
		leftMap = new MultiValueMap();
		rightMap = new MultiValueMap();
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		List<Gate> gates = getParsedGates();
		System.out.println(gates.size());
		

		List<List<Gate>> layersOfGates = getLayersOfGates(gates);
		int i = 0;
		for(List<Gate> list: layersOfGates){
			i = i + list.size();
		}
		System.out.println(i);

		outputSortedGates(layersOfGates);
		System.out.println("Took: " + ((System.currentTimeMillis() - startTime) / 1000)
				+ " sec");
	}

	/**
	 * @param inputFile
	 * @param charset
	 * @return
	 */
	public List<Gate> getParsedGates() {
		boolean counter = false;
		ArrayList<Gate> res = new ArrayList<Gate>();
		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(inputFile), charset));
			String line = "";
			while((line = fbr.readLine()) != null) {
				/*
				 * Ignore blank lines
				 */
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
					counter = false;
					continue;
				}

				/*
				 * Parse each gate line and count numberOfNonXORGates
				 */
				Gate g = new Gate(line);
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
	 * @param sortedLeft
	 * @param sortedRight
	 * @return
	 */
	public List<List<Gate>> getLayersOfGates(List<Gate> gates) {
		List<List<Gate>> layersOfGates = new ArrayList<List<Gate>>();

		for(Gate g: gates){
			leftMap.put(g.getLeftWireIndex(), g);
			rightMap.put(g.getRightWireIndex(), g);
		}
		System.out.println(leftMap.values().size());
		System.out.println(rightMap.values().size());

		//TODO Rewrite code so non-recursive, only look at immedient dependors of your
		// this child
//		int j = 0;
		for(int i = 0; i < 256; i++){
			Collection<Gate> leftList = leftMap.getCollection(i);
			System.out.println(leftList);
			if(leftList == null){
				continue;
			}
			for(Gate g: leftList){
//				j++;
//				System.out.println(j);
				evalGate(g, 0, layersOfGates);
			}
		}
//		System.out.println("-------");
//		int k = 0;
		for(int i = 0; i < 256; i++){
			Collection<Gate> rightList = rightMap.getCollection(i);
			if(rightList == null){
				continue;
			}
			System.out.println(rightList);
			for(Gate g: rightList){
//				k++;
//				System.out.println(k);
				evalGate(g, 0, layersOfGates);
			}
		}
		return layersOfGates;
	}

	/**
	 * @param g
	 * @param time
	 * @param list
	 * @param layersOfGates
	 */
	private void evalGate(Gate g, int time, List<List<Gate>> layersOfGates) {
		g.decCounter();
		if (g.getCounter() == 0){
			g.setTime(time);
			addToSublist(g, layersOfGates);
			List<Gate> outputGates = getOutputGates(g);

			for(Gate outputGate: outputGates){
				evalGate(outputGate, time + 1, layersOfGates);
			}
		} 
	}

	/**
	 * @param g
	 * @param sortedList
	 * @return
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
	 * @return
	 */
	private List<List<Gate>> addToSublist(Gate g, List<List<Gate>> gates){
		
		while(gates.size() <= g.getTime()){
			gates.add(new ArrayList<Gate>());
		}
		
		List<Gate> layer = gates.get(g.getTime());
		layer.add(g);

		return gates;
	}

	/**
	 * @param multiTimedGates
	 */
	private void outputSortedGates(List<List<Gate>> sortedGates) {
		BufferedWriter fbw = null;
		try {
			fbw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), charset));
			String header = getHeader(sortedGates);
			fbw.write(header);
			fbw.newLine();

			/*
			 * Write the gates the the file, one layer at a time
			 */
			for(List<Gate> l: sortedGates){
				// Write the size of the current layer
				fbw.write("*" + l.size()); 
				fbw.newLine();

				// Write the gates in this layer
				for(Gate g: l){
					String gateString = sortedGates.indexOf(l) + " " + g.toString();
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
		int numberOfTotalInputs = numberOfAliceInputs + numberOfBobInputs;
		int numberOfTotalOutputs = numberOfTotalInputs/2;
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
		String header = numberOfTotalInputs + " " + numberOfTotalOutputs + " " +
				numberOfWires + " " + numberOfLayers + " " + maxLayerWidth +
				" " + numberOfNonXORGates;

		return header;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 1){
			System.out.println("Incorrect number of arguments, please specify inputfile");
			return;
		}

		String inputfileName = null, outputfileName = null;

		for(int param = 0; param < args.length; param++){
			if (inputfileName == null) {
				inputfileName = args[param];
			}

			else if (outputfileName == null) {
				outputfileName = args[param];
			}

			else System.out.println("Unparsed: " + args[param]); 
		}
		if(outputfileName == null) {
			outputfileName = "out.txt";
		}
		File inputFile = new File(inputfileName);

		File outputFile = new File(outputfileName);

		if (!inputFile.exists()){
			System.out.println("Inputfile: " + inputFile.getName() + " not found");
			return;
		}

		CircuitSorter sorter = new CircuitSorter(inputFile, outputFile);
		sorter.run();
	}
}
