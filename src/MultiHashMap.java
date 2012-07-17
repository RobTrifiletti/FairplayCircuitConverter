import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MultiHashMap extends HashMap<Integer, List<Gate>> {

  public void put(int key, Gate g) {
    List<Gate> current = get(key);
        if (current == null) {
            current = new ArrayList<Gate>();
            super.put(key, current);
        }
        current.add(g);
    }
}
