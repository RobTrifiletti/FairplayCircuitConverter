import java.util.Comparator;


public class GateTimeComparator implements Comparator<Gate> {

	public int compare(Gate g0, Gate g1) {
		if (g0.getTime() > g1.getTime()){
			return 1;
		}
		else if (g0.getTime() < g1.getTime()){
			return -1;
		}
		else return 0;
	}


}
