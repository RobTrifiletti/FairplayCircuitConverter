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
import java.util.HashSet;
import java.util.List;


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

	/**
	 * @param inputfileName
	 * @param outputfileName
	 */
	public CircuitSorter(String inputfileName, String outputfileName) {
		charset = Charset.defaultCharset();
		this.inputFile = new File(inputfileName);
		if (!inputFile.exists()){
			System.out.println("Inputfile: " + inputfileName + " not found");
			return;
		}
		this.outputFile = new File(outputfileName);
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		List<Gate> gates = getParsedGates(inputFile, charset);

		MultiHashMap leftMap = new MultiHashMap();
		MultiHashMap rightMap = new MultiHashMap();
		for(Gate g: gates){
			leftMap.put(g.getLeftWireIndex(), g);
			rightMap.put(g.getRightWireIndex(), g);
		}

		List<List<Gate>> multiTimedGates = getTimestampedGates(leftMap, rightMap);

		outputSortedGates(multiTimedGates);
		System.out.println("Took: " + ((System.currentTimeMillis() - startTime) / 1000)
				+ " sec");
	}

	/**
	 * @param inputFile
	 * @param charset
	 * @return
	 */
	private List<Gate> getParsedGates(File inputFile, Charset charset) {
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
	private List<List<Gate>> getTimestampedGates(MultiHashMap leftMap,
			MultiHashMap rightMap) {
		List<List<Gate>> multiTimedGates = new ArrayList<List<Gate>>();

		int j = 0;
		for(int i = 0; i < 256; i++){
			List<Gate> leftList = leftMap.get(i);
			if(leftList == null){
				continue;
			}
			for(Gate g: leftList){
				j++;
				System.out.println(j);
				evalGate(g, 0, leftMap, rightMap, multiTimedGates);
			}
		}
		System.out.println("-------");
		int k = 0;
		for(int i = 0; i < 256; i++){
			List<Gate> rightList = rightMap.get(i);
			if(rightList == null){
				continue;
			}
			for(Gate g: rightList){
				k++;
				System.out.println(k);
				evalGate(g, 0, leftMap, rightMap, multiTimedGates);
			}
		}
		return multiTimedGates;
	}

	/**
	 * @param g
	 * @param time
	 * @param list
	 * @param multiTimedGates
	 */
	private void evalGate(Gate g, int time, MultiHashMap leftMap, MultiHashMap rightMap,
			List<List<Gate>> multiTimedGates) {
		g.decCounter();
		if (g.getCounter() == 0){
			g.setTime(time);
			addToSublist(g, multiTimedGates);
			List<Gate> outputGates = getOutputGates(g, leftMap, rightMap);

			for(Gate outputGate: outputGates){
				evalGate(outputGate, time + 1, leftMap, rightMap, multiTimedGates);
			}
		} 
	}

	/**
	 * @param g
	 * @param sortedList
	 * @return
	 */
	private List<Gate> getOutputGates(Gate g, MultiHashMap leftMap, 
			MultiHashMap rightMap){
		List<Gate> res = new ArrayList<Gate>();
		int inputIndex = g.getOutputWireIndex();
		List<Gate> leftList = leftMap.get(inputIndex);
		List<Gate> rightList = rightMap.get(inputIndex);

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
	public List<List<Gate>> addToSublist(Gate g, List<List<Gate>> gates){
		if (gates.size() <= g.getTime()){
			ArrayList<Gate> l = new ArrayList<Gate>();
			l.add(g);
			gates.add(l);
		}
		else{
			List<Gate> l = gates.get(g.getTime());
			l.add(g);
		}

		return gates;
	}

	/**
	 * @param multiTimedGates
	 */
	private void outputSortedGates(List<List<Gate>> multiTimedGates) {
		BufferedWriter fbw = null;
		try {
			fbw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), charset));

			int numberOfTotalInputs = numberOfAliceInputs + numberOfBobInputs;
			int numberOfTotalOutputs = numberOfTotalInputs/2;
			int numberOfWires = getNumberOfWires(multiTimedGates);
			int numberOfLayers = multiTimedGates.size();

			int maxLayerWidth = 0;

			/*
			 * We have to figure out the max layer size before writing to the file.
			 */
			for(List<Gate> l: multiTimedGates){
				maxLayerWidth = Math.max(maxLayerWidth, l.size());
			}

			/*
			 * Build and write the header of the output file
			 */
			String header = numberOfTotalInputs + " " + numberOfTotalOutputs + " " +
					numberOfWires + " " + numberOfLayers + " " + maxLayerWidth +
					" " + numberOfNonXORGates;
			fbw.write(header);
			fbw.newLine();

			/*
			 * Write the gates the the file, one layer at a time
			 */
			for(List<Gate> l: multiTimedGates){
				// Write the size of the current layer
				fbw.write("*" + l.size()); 
				fbw.newLine();

				// Write the gates in this layer
				for(Gate g: l){
					String gateString = multiTimedGates.indexOf(l) + " " + g.toString();
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

		CircuitSorter sorter = new CircuitSorter(inputfileName, outputfileName);
		sorter.run();
	}
}
