import java.util.ArrayList;
import java.util.Arrays;
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
	
	//TODO: Complete method (may outright delete, looking likely atm)
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
	//TODO: do we want to refactor the storage for logging purposes? and how will we do it

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
     * @param root root node of the B+ Tree
     * @param record record to be inserted
     * @param pointer address of record to be inserted
     * @return root root node of the B+ tree
     */
    public static Node insert(Node root, Record record, RecordPointer pointer) {
    	// Create new tree if root is null
    	if(root == null) {
    		root = new LeafNode(true);
    	}
    	
        int key = record.getNumVotes();
        // Insert by traversing the tree from the root node
        KeyNode newRoot = root.insertInternal(key, pointer, null);
        return newRoot.getNode();
    }

    /**
     * Insertion in B+ tree (recursive)
     * @param node current node
     * @param entry entry to be inserted
     * @param splitChild key-node pair which points to split child, null if child was not split
     * @return a KeyNode of either the new root or the split child if current node was split, otherwise null
     */
    public KeyNode insertInternal(int key, RecordPointer pointer, KeyNode splitChild) {
    	boolean split = false;
        if (this instanceof InternalNode) {
            InternalNode curNode = (InternalNode) this;

            // Find index of pointer to leftmost node that can be inserted with the entry
            int child = curNode.findIndexOfNode(key);

            // Insert entry to subtree
            splitChild = curNode.getPointers()[child].insertInternal(key, pointer, splitChild);

            if (splitChild != null) {
            	splitChild.getNode().setParent(curNode);
                if (curNode.getDegree() < getN()+1) {
                    // Insert entry to node if it is not full
                    curNode.addSorted(splitChild.getKey(), splitChild.getNode());
                    //TODO: figure out if this is necessary or redundant
                    splitChild = null;
                } else {
                    // Split node if it is full
                	splitChild = splitNode(curNode, splitChild);
                    split = true;

                    // Increase number of nodes
                    ++totalNodes;
                }
            }
        } else if (this instanceof LeafNode) {
            LeafNode leafNode = (LeafNode) this;
            if (leafNode.getDegree() < getN()) {
                // Add entry to leaf node if it is not full
                leafNode.addSorted(key, pointer);
                //TODO: figure out if this is necessary or redundant
                splitChild = null;
            } else {
                // Split leaf if it is full
                splitChild = splitLeaf(leafNode, key, pointer);
                split = true;

                //TODO: Confirm that this can be tracked by degree; add recursive function to get this
                // Increase number of nodes
                ++totalNodes;
            }
        }

        if (split && this.isRoot()) {
            // If root is split, add a new node to be the root
            InternalNode newNode = new InternalNode(true);
            newNode.addPointer(this, newNode.getDegree());
            newNode.addSorted(splitChild.getKey(), splitChild.getNode());
            this.setParent(newNode);
            splitChild.getNode().setParent(newNode);
            //TODO: finish debugging and delete this
            if(this.getHeight() != splitChild.getNode().getHeight()) {
            	System.out.println("Height tracking fked up somewhere");
            }
            newNode.setHeight(this.getHeight()+1);

            //TODO: Confirm that this can be tracked by degree; add recursive function to get this
            // Increase number of nodes
            ++totalNodes;
            return new KeyNode(0, newNode);
        }
        return splitChild;
    }

    //TODO: REFACTOR
    /**
     * Split full internal node into two parts
     * @param node internal node to be split
     * @param knPair key and pointer to be added
     * @return pair of the smallest key in second node and pointer to second node
     */
    public KeyNodePair splitNode(InternalNode node, KeyNodePair knPair) {
        Key[] keys = node.getKeys();
        Node[] pointers = node.getPointers();

        // Create temporary array to store existing and to be added keys and pointers
        Key[] tempKeys = Arrays.copyOf(keys, keys.length+1);
        Node[] tempPointers = Arrays.copyOf(pointers, pointers.length+1);

        // Find midpoint to split node
        int mid = (int) Math.floor((n+1)/2.0);

        // Find on which index the key and pointer can be inserted in order to keep it sorted
        int indexToInsertKey = Util.findIndexToInsert(tempKeys, knPair.getKey());

        // Insert key and pointer to temporary array
        Util.insertAndShift(tempKeys, knPair.getKey(), indexToInsertKey);
        Util.insertAndShift(tempPointers, knPair.getNode(), indexToInsertKey+1);

        // Split key and pointer arrays in half
        Key[] firstHalfKeys = Arrays.copyOfRange(tempKeys, 0, mid);
        Node[] firstHalfPointers = Arrays.copyOfRange(tempPointers, 0, mid+1);
        Key[] secondHalfKeys = Arrays.copyOfRange(tempKeys, mid+1, tempKeys.length);
        Node[] secondHalfPointers = Arrays.copyOfRange(tempPointers, mid+1, tempPointers.length);

        // Set keys and pointers to nodes
        node.setKeys(Arrays.copyOf(firstHalfKeys, keys.length));
        node.setPointers(Arrays.copyOf(firstHalfPointers, pointers.length));
        node.setDegree(firstHalfPointers.length);

        // Create a new node to store the split keys and pointers
        InternalNode newNode = new InternalNode(secondHalfPointers.length, Arrays.copyOf(secondHalfKeys, keys.length),
                Arrays.copyOf(secondHalfPointers, pointers.length));

        // Set the new node as parent of moved nodes
        for (int i = 0; i < newNode.getDegree(); ++i) {
            newNode.getPointers()[i].setParent(newNode);
        }

        // Return pair of the smallest key in second node and pointer to second node
        return new KeyNodePair(tempKeys[mid], newNode);
    }

    //TODO: REFACTOR
    /**
     * Split full leaf node into two parts
     * @param node leaf node to be split
     * @param entry entry to be added
     * @return pair of the smallest key in second node and pointer to second node
     */
    public KeyNodePair splitLeaf(LeafNode node, KeyValuePair entry) {
        KeyValuePair[] kvPairs = node.getKvPairs();

        // Create a temporary array to store the existing and to be added entry
        KeyValuePair[] temp = Arrays.copyOf(kvPairs, kvPairs.length+1);

        // Find midpoint to split node
        int mid = (int) Math.ceil((n+1)/2.0);

        // Find on which index the entry can be inserted to kvPairs in order to keep it sorted
        int indexToInsert = Util.findIndexToInsert(temp, entry);

        // Insert key-value pair
        Util.insertAndShift(temp, entry, indexToInsert);

        // Split key-value pair array into half
        KeyValuePair[] firstHalf = Arrays.copyOfRange(temp, 0, mid);
        KeyValuePair[] secondHalf = Arrays.copyOfRange(temp, mid, temp.length);

        // Set key-value pairs to nodes
        node.setKvPairs(Arrays.copyOf(firstHalf, kvPairs.length));
        node.setDegree(firstHalf.length);

        // Create a new node to store the split key-value pairs
        LeafNode newLeaf = new LeafNode(secondHalf.length, Arrays.copyOf(secondHalf, kvPairs.length));

        // Modify sibling relations on leaf nodes
        LeafNode rightSibling = node.getRightSibling();
        node.setRightSibling(newLeaf);
        newLeaf.setRightSibling(rightSibling);
        newLeaf.setLeftSibling(node);
        if (rightSibling != null) rightSibling.setLeftSibling(newLeaf);

        // Return pair of the smallest key in second node and pointer to second node
        return new KeyNodePair(newLeaf.getKvPairs()[0].getKey(), newLeaf);
    }
    
	//TODO: helper functions (mostly done)

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
	
    /**
     * Recursively find the total number of nodes in a B+ tree
     * @param root root node of the current subtree
     * @return total number of nodes from the subtree (inclusive of root)
     */
    public static int getTotalNodes(Node root) {
    	if(root instanceof LeafNode) {
    		return 1;
    	}
    	int total = 0;
    	InternalNode root1 = (InternalNode) root;
    	for(int i = 0; i < root1.getDegree(); i++) {
    		total += getTotalNodes(root1.getPointers()[i]);
    	}
    	return total+1;
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