/**
 * Interface representing a node in a B+ tree
 */
public abstract class Node {
	/**
	 * Maximum number of keys that can be held
	 */
	private static int n;
	/**
	 * Height of the current node
	 */
	private int height;
	/**
	 * Number of child nodes/ entries
	 */
	private int degree;
	/**
	 * Whether the node is the root of the tree
	 */
	private boolean isRoot;
	/**
	 * Array of keys
	 */
	private int[] keys;
	/**
	 * Parent node
	 */
	private InternalNode parent;
	
	/**
	 * Constructs a BPlusTree.
	 * @return the root node of that tree
	 */
	public static LeafNode createTree(Record record, int n) {
		return new LeafNode(true);
	}
	
	/**
	 * Node constructor
	 * @param n maximum number of keys 
	 * @param height height of current node
	 * @param degree current degree of node
	 * @param isRoot whether the node is root node
	 * @param keys array of keys
	 * @param parent parent node
	 */
	public Node(int height, int degree, boolean isRoot, int[] keys, InternalNode parent) {
		this.height = height;
		this.degree = degree;
		this.isRoot = isRoot;
		this.keys = keys;
		this.parent = parent;
	}
	
    //TODO
	/**
     * Find index to insert a new key to the array of keys
     * @param k key to be inserted
     * @return index to insert
     */
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
    
    /**
     * Insert a key to a specific index in the array of keys, shift the keys affected by the insertion
     * and delete last key in the array
     * @param key key to be inserted
     * @param pos index to insert
     */
    public void insertAndShift(int key, int pos) {
        for (int i = keys.length - 1; i > pos; i--) {
            keys[i] = keys[i-1];
        }
        keys[pos] = key;
    }

    /**
     * Delete a key on the specified index in the array of keys, then shift the keys accordingly
     * @param pos position of key to be deleted
     */
    public void deleteAndShift(int pos) {
        for (int i = pos; i < keys.length - 1; i++) {
            keys[i] = keys[i+1];
        }
        keys[keys.length - 1] = 0;
    }
	
	public static int getN() {
		return Node.n;
	}
	
	public static void setN(int n) {
		Node.n = n;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
    public int getDegree() {
    	return this.degree;
    }

    public void setDegree(int degree) {
    	this.degree = degree;
    }
    
    public boolean isRoot() {
    	return this.isRoot;
    }
    
    public void setRoot(boolean isRoot) {
    	this.isRoot = isRoot;
    }

    public int[] getKeys() {
    	return this.keys;
    }
    
    public void setKeys(int[] keys) {
    	this.keys = keys;
    }
    
    public InternalNode getParent() {
    	return this.parent;
    }

    public void setParent(InternalNode parent) {
    	this.parent = parent;
    }

    public abstract String toString();
}