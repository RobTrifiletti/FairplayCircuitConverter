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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


public class CircuitSorter implements Runnable {

	private Charset charset;
	private File inputFile;
	private File outputFile;
	private List<Gate> gates;
//	private List<Gate> timedGates;
	private List<List<Gate>> multiTimedGates;

	private int numberOfGates = 0;
	private int numberOfWires = 0;
	private int numberOfAliceInputs = 0;
	private int numberOfBobInputs = 0;
	private int numberOfAliceOutputs = 0;
	private int numberOfBobOutputs = 0;
	private int numberOfNonXORGates = 0;

	public CircuitSorter(String inputfileName, String outputfileName) {
		charset = Charset.defaultCharset();
		this.inputFile = new File(inputfileName);
		if (!inputFile.exists()){
			System.out.println("Inputfile: " + inputfileName + " not found");
			return;
		}
		this.outputFile = new File(outputfileName);
		multiTimedGates = new ArrayList<List<Gate>>();
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		gates = parseFile(inputFile, charset);

		List<Gate> sortedLeft = sortGates(gates, new WireComparator(WireSortEnum.LEFT));
		List<Gate> sortedRight = sortGates(gates, new WireComparator(WireSortEnum.RIGHT));

		runThroughGates(sortedLeft, sortedRight);
		outputSortedGates();
		System.out.println("Took: " + ((System.currentTimeMillis() - startTime) / 1000)
				+ " sec");
	}
	
	private List<Gate> parseFile(File inputFile, Charset charset) {
		boolean counter = false;
		ArrayList<Gate> res = new ArrayList<Gate>();
		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(inputFile), charset));
			String line = "";
			while((line = fbr.readLine()) != null) {
				if (line.isEmpty()){
					continue;
				}
				if(line.matches("[0-9]* [0-9]*")){
					String[] split = line.split(" ");
					numberOfGates = Integer.parseInt(split[0]);
					numberOfWires = Integer.parseInt(split[1]);
					counter = true;
					continue;
				}
				if (counter == true){
					String[] split = line.split(" ");
					numberOfAliceInputs = Integer.parseInt(split[0]);
					numberOfBobInputs = Integer.parseInt(split[1]);
					numberOfAliceOutputs = Integer.parseInt(split[4]);
					numberOfBobOutputs = Integer.parseInt(split[5]);

					counter = false;
					continue;
				}
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
	

	private List<Gate> sortGates(List<Gate> gates, WireComparator wireComparator) {
		Collections.sort(gates, wireComparator);
		return gates;
	}
	
	private void runThroughGates(List<Gate> sortedLeft, List<Gate> sortedRight) {
		for(Gate g: sortedLeft){
			if (g.getLeftWireIndex() < 256){
				evalGate(g, 0, sortedLeft);
			}
			else continue;
		}
		for(Gate g: sortedRight){
			if (g.getRightWireIndex() < 256){
				evalGate(g, 0, sortedRight);
			}
			else continue;
		}
	}

	private void evalGate(Gate g, int time, List<Gate> list) {
		g.decCounter();
		if (g.getCounter() == 0){
			g.setTime(time);
			addToSublist(g, multiTimedGates);
			List<Gate> outputGates = getOutputGates(g, list);
			for(Gate outputGate: outputGates){
				evalGate(outputGate, time + 1, list);
			}
		}

	}


	private void outputSortedGates() {
		BufferedWriter fbw = null;
		try {
			fbw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), charset));

			int numberOfTotalInputs = numberOfAliceInputs + numberOfBobInputs;
			int numberOfTotalOutputs = numberOfTotalInputs/2;
			int numberOfWires = getNumberOfWires(multiTimedGates);
			int numberOfLayers = multiTimedGates.size();
			
			int maxLayerWidth = 0;
			
			// Have to figure out the max layer size before writing the file
			for(List<Gate> l: multiTimedGates){
				maxLayerWidth = Math.max(maxLayerWidth, l.size());
			}
			String header = numberOfTotalInputs + " " + numberOfTotalOutputs + " " +
					numberOfWires + " " + numberOfLayers + " " + maxLayerWidth +
					" " + numberOfNonXORGates;
			
			fbw.write(header);
			fbw.newLine();
			
			for(List<Gate> l: multiTimedGates){
				fbw.write("*" + l.size()); // #GatesInCurrentLayer
				
				fbw.newLine();
				for(Gate g: l){
					String gateString = multiTimedGates.indexOf(l) + " " + g.toString();

					fbw.write(gateString);
					fbw.newLine();
				}
			}
//			RandomAccessFile file = new RandomAccessFile("out.txt", "rws");
//		    byte[] text = new byte[(int) file.length()];
//		    file.readFully(text);
//		    file.seek(0);
//		    file.writeBytes(header);
//		    file.write(text);
//		    file.close();
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

	private List<Gate> getOutputGates(Gate g, List<Gate> sortedList){
		//TODO Optimize
		List<Gate> res = new ArrayList<Gate>();
		for(Gate candidateGate: sortedList){
			int outputIndex = g.getOutputWireIndex();
			if (outputIndex == candidateGate.getLeftWireIndex()
					|| outputIndex == candidateGate.getRightWireIndex()){
				res.add(candidateGate);
			}
		}
		return res;
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

}
