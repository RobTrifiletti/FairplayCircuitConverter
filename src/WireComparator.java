import java.util.Comparator;


public class WireComparator implements Comparator<Gate> {

	private static final String ignoredPrefix = "2 1 ";
	private WireSortEnum compareByWire;

	public WireComparator(WireSortEnum compareByWire){
		this.compareByWire = compareByWire;
	}

	public int compare(Gate g0, Gate g1) {
		int i0, i1;
		switch(compareByWire){
		case LEFT:
			i0 = g0.getLeftWireIndex();
			i1 = g1.getRightWireIndex();
			break;
		case RIGHT:
			i0 = g0.getRightWireIndex();
			i1 = g1.getRightWireIndex();
			break;
		case OUTPUT:
			i0 = g0.getOutputWireIndex();
			i1 = g1.getOutputWireIndex();
			break;
		default:
			i0 = i1 = 0;
		}

		if (i0 > i1){
			return 1;
		}
		if (i0 < i1){
			return -1;
		}
		else return 0;
	}
}
