import java.io.File;


public class Driver {
	/**
	 * @param args
	 * Should be circuitFile.txt outputfilename.txt
	 * If no output filename is given, out.txt is chosen by default
	 * circuitFile.txt must exist on the file system, else error
	 * Optional: add a -t argument to get the running time of the conversion in
	 * standard out
	 */
	public static void main(String[] args) {
		if(args.length < 1){
			System.out.println("Incorrect number of arguments, please specify inputfile");
			return;
		}

		boolean timed = false;
		boolean sorted = false;
		String circuitFilename = null;
		String outputFilename = null;

		/*
		 * Parse the arguments
		 */
		for(int param = 0; param < args.length; param++){
			if(args[param].equals("-t")){
				timed = true;
			}
			if(args[param].equals("-s")){
				sorted = true;
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

		CircuitConverter converter = 
				new CircuitConverter(circuitFile, outputFile, timed, sorted);
		converter.run();
	}
}
