import java.util.ArrayList;
import java.util.List;

public class Tree {
    private Node root;

    public Tree(String rootData) {
        root = new Node();
        root.data = rootData;
        root.children = new ArrayList<Node>();
    }

    private class Node {
        private String data;
        private Node parent;
        private List<Node> children;
    }
}