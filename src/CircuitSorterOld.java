import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;


public class CircuitSorterOld implements Runnable {

	private Charset charset;
	private List<String> lines;
	//	private ArrayList<String> layer0;
	//	private ArrayList<String> layer1;
	private ArrayList<List<String>> layers;
	private ArrayList<Integer> computed;
	private Map<Integer, GateOld> outputHash;
	//	private Map<Integer, String> leftHash;
	//	private Map<Integer, String> rightHash;
	private List<GateOld> gates;
	private File inputFile;
	private File outputFile;


	public CircuitSorterOld(String inputfileName, String outputfileName) {
		charset = Charset.defaultCharset();
		lines = new CopyOnWriteArrayList<String>();
		outputHash = new HashMap<Integer, GateOld>();
		//		layer0 = new ArrayList<String>();
		//		layer1 = new ArrayList<String>();
		layers = new ArrayList<List<String>>();
		computed = new ArrayList<Integer>();
		//		leftHash = new HashMap<Integer, String>();
		//		rightHash = new HashMap<Integer, String>();
		gates = new ArrayList<GateOld>();
		this.inputFile = new File(inputfileName);
		if (!inputFile.exists()){
			System.out.println("Inputfile: " + inputfileName + " not found");
			return;
		}
		this.outputFile = new File(outputfileName);
	}


	public void run() {
		parseFile();
		//		hashStrings();
		buildCircuit();
	}

//	private void buildCircuit(){
//		for(String s: lines){
//			String [] split = s.split(" ");
//			int outputIndex = Integer.parseInt(split[4]);
//			Gate g = new Gate(s);
//			gates.add(g);
//			outputHash.put(outputIndex, g);
//		}
//		System.out.println("hey");
//		while(gates.size() > 1){
//			Random random = new Random();
//			Gate g = gates.get(random.nextInt(gates.size()));
//			
//			Gate leftGate = outputHash.get(g.getLeftWire());
//			Gate rightGate = outputHash.get(g.getRightWire());
//			System.out.println(leftGate.getGateString());
//			System.out.println(rightGate.getGateString());
//			gates.remove(leftGate);
//			gates.remove(rightGate);
//			gates.remove(g);
//			
//			Gate gate = new Gate(leftGate, rightGate, g.getGateString());
//			gates.add(gate);
//			
//			System.out.println(gates.size());
//		}
//	}

		private void buildCircuit() {
			ArrayList<Integer> tmpComputed = new ArrayList<Integer>();
			for (int i = 0; i < 256; i++){
				computed.add(i);
			}
			
			while(!lines.isEmpty()){
				ArrayList<String> layer = new ArrayList<String>();
				for(String s: lines){
					if (computed.contains(getLeftWireIndex(s))
							&& computed.contains(getRightWireIndex(s))){
						layer.add(s);
						tmpComputed.add(getOutputWireIndex(s));
						lines.remove(s);
					}
				}
				layers.add(layer);
				computed.addAll(tmpComputed);
				tmpComputed.clear();
				//System.out.println(layers.size()); // 288 layers 482
			}
			for(String s: layers.get(layers.size() - 1)){
				System.out.println(s);
			}
		}


	private void parseFile() {
		try {
			BufferedReader fbr = new BufferedReader(new InputStreamReader(
					new FileInputStream(inputFile), charset));
			String line = "";
			while((line = fbr.readLine()) != null) { 
				lines.add(line);
			}
			fbr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private int getLeftWireIndex(String s){
		String[] split = s.split(" ");
		return Integer.parseInt(split[2]);
	}

	private int getRightWireIndex(String s){
		String[] split = s.split(" ");
		return Integer.parseInt(split[3]);
	}

	private int getOutputWireIndex(String s){
		String[] split = s.split(" ");
		return Integer.parseInt(split[4]);
	}

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

		CircuitSorterOld sorter = new CircuitSorterOld(inputfileName, outputfileName);
		sorter.run();
	}
}
