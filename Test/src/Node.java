import java.util.ArrayList;
import java.util.List;

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
	
	//TODO: Complete method
	/**
	 * Constructs a BPlusTree.
	 * @return the root node of that tree
	 */
	public static LeafNode createTree(Record record) {
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
     * Search for records with the specified value
     * @param key search key (numVotes)
     * @return a list of record addresses with a key value equal to the search key
     */
    public ArrayList<RecordPointer> search(int key) {
        // Reset logs for experiment
        storage.resetLog();

        // Since there could be more than one result for a search key, searching for a single key can be done
        // by using range search, with the search key as both the lower and upper bound
        return searchInternal(null, key, key);
    }

    /**
     * Search for records with the value within the lower and upper bounds
     * @param lower lower bound of the search key, inclusive (numVotes)
     * @param upper upper bound of the search key, inclusive (numVotes)
     * @return a list of record addresses with a key value ranging from the lower to the upper bounds
     */
    public ArrayList<RecordPointer> search(int lower, int upper) {
        // Reset logs for experiment
        storage.resetLog();

        return searchInternal(null, lower, upper);
    }

	//TODO: Rename searchInternal to bPlusSearch
    /**
     * Searching in the B+ tree
     * @param results list for storing results
     * @param lower lower bound of search
     * @param upper upper bound of search
     * @return a list of record addresses with a key value ranging from the lower to the upper bounds
     */
    public ArrayList<RecordPointer> searchInternal(ArrayList<RecordPointer> results, int lower, int upper) {
    	if(results == null) {
    		results = new ArrayList<RecordPointer>();
    	}
        if (this instanceof LeafNode) {
            // Iterate through leaf node to find all occurrences of search key
            LeafNode node = (LeafNode) this;
            // Record node access here, since leaf nodes can be traversed through siblings
            storage.logNodeAccess(node);

            int[] keys = node.getKeys();

            for (int i = 0; i < node.getDegree(); i++) {
            	// Add to result if current key value is within lower and upper bounds
                // Finish search if it is higher than the upper bound
                if (lower <= keys[i] && keys[i] <= upper) {
                	results.add(node.getPointers()[i]);
                } else if (upper < keys[i]) {
                	return results;
                }
            }
            // Iterate to right sibling of leaf node
            node.getRightSibling().searchInternal(results, lower, upper);

        } else if (this instanceof InternalNode) {
        	InternalNode node = (InternalNode) this;
            // Record node access
            storage.logNodeAccess(node);

            // Traverse to the leftmost subtree possibly containing the lower bound
            int child = node.findIndexOfNode(lower);
            node.getPointers()[child].searchInternal(results, lower, upper);
        }
        return results;
    }

    /**
     * Insert to B+ tree with numVotes as key
     * Value of the entry is the logical address of the record (Block ID, Record ID)
     * @param record record to be inserted
     * @param pointer address of record to be inserted
     * @return 
     */
    public static Node insert(Node root, Record record, RecordPointer pointer) {
    	if(root == null) {
    		root = createTree(record, )
    	}
        int key = record.getNumVotes();

        // Insert by traversing the tree from the root node
        insertInternal(root, key, pointer, 0, null);
        return root;
    }

    /**
     * Insertion in B+ tree (recursive)
     * @param node current node
     * @param entry entry to be inserted
     * @param newChildEntry key-node pair which points to split child, null if child was not split
     * @return a key-node pair if current node is split, otherwise null
     */
    public KeyNodePair insertInternal(Node node, int recordKey, RecordPointer recordPointer, int nodeKey, Node newChildEntry) {
        if (root == null) {
            root = new LeafNode(n);
            node = root;
        }

        boolean split = false;
        if (node instanceof InternalNode) {
            InternalNode curNode = (InternalNode) node;

            // Find index of pointer to leftmost node that can be inserted with the entry
            int pointerIndex = findIndexOfNode(curNode, entry.getKey());

            // Insert entry to subtree
            newChildEntry = insertInternal(curNode.getPointers()[pointerIndex], entry, newChildEntry);

            if (newChildEntry != null) {
                newChildEntry.getNode().setParent(curNode);
                if (curNode.getDegree() < maxDegreeInternal) {
                    // Insert entry to node if it is not full
                    curNode.addSorted(newChildEntry);
                    newChildEntry = null;
                } else {
                    // Split node if it is full
                    newChildEntry = splitNode(curNode, newChildEntry);
                    split = true;

                    // Increase number of nodes
                    ++totalNodes;
                }
            }

        } else if (node instanceof LeafNode) {
            LeafNode leafNode = (LeafNode) node;
            if (leafNode.getDegree() < maxKeysLeaf) {
                // Add entry to leaf node if it is not full
                leafNode.addSorted(entry);
                newChildEntry = null;
            } else {
                // Split leaf if it is full
                newChildEntry = splitLeaf(leafNode, entry);
                split = true;

                // Increase number of nodes
                ++totalNodes;
            }
        }

        if (split && root == node) {
            // If root is split, add a new node to be the root
            InternalNode newNode = new InternalNode(n);
            newNode.addPointer(node, newNode.getDegree());
            newNode.addSorted(newChildEntry);
            node.setParent(newNode);
            newChildEntry.getNode().setParent(newNode);
            root = newNode;

            // Increase height of tree and number of nodes
            ++height;
            ++totalNodes;
        }

        return newChildEntry;
    }
    
	//TODO: helper functions

    //TODO: Rename to findIndexInParent
    /**
     * Find index of pointer to node in its parent node
     * @param parentNode parent of node
     * @param node node
     * @return index of node
     */
    public int findIndexOfPointer(InternalNode parentNode, Node node) {
        Node[] pointers = parentNode.getPointers();
        int i;
        for (i = 0; i < pointers.length; ++i) {
            if (pointers[i] == node) break;
        }
        return i;
    }
    
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