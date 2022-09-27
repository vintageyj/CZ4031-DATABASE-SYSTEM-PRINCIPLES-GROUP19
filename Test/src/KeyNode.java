/**
 * Class representing a pair of key and pointer to node (wrapper class for returning values)
 */
public class KeyNode {

    /**
     * Key
     */
    private int key;

    /**
     * Pointer to node
     */
    private Node node;

    /**
     * Construct a key-node pair
     * @param key key
     * @param node pointer to node
     */
    public KeyNode(int key, Node node) {
        this.key = key;
        this.node = node;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}