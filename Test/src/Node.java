/**
 * Interface representing a node in a B+ tree
 */
public abstract class Node {
	/**
	 * Maximum number of keys that can be held
	 */
	private int n;
	/**
	 * Height of the current node
	 */
	private int height;
	/**
	 * Whether the node is the root of the tree
	 */
	private boolean isRoot;
	/**
	 * Array of keys
	 */
	private int[] keys;
	
	/**
	 * Constructs a BPlusTree.
	 * @return the root node of that tree
	 */
	public static Node createTree(Record record, int n) {
		return new LeafNode(n, 0);
	}
	
	public Node(int n, int height) {
		this.n = n;
		this.height = height;
	}
	
	public int getN() {
		return this.n;
	}
	
	public int getHeight() {
		return this.height;
	}
	
    public abstract int getDegree();

    public abstract void setDegree(int degree);

    public abstract InternalNode getParent();

    public abstract void setParent(InternalNode parent);
    
    //TODO
    
    public int findIndexToInsert(int k) {
    	int low = 0, high = keys.length-1, mid;
        while (low < high) {
            mid = low + (high - low) / 2;
            if (k == keys[mid]) {
                return mid;
            } else if (k < keys[mid]) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return k > keys[low] ? low+1 : low;
	}

    public abstract String toString();
}