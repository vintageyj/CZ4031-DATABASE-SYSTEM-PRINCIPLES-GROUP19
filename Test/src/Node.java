import java.util.ArrayList;

/**
 * Interface representing a node in a B+ tree
 */
public abstract class Node {
	/**
	 * Storage for logging purposes
	 */
	private static Storage storage;
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
        KeyNode newRoot = root.insertInternal(key, pointer);
        return newRoot.getNode();
    }

    //TODO: Rename insertInternal to bPlusInsert
    /**
     * Insertion in B+ tree (recursive)
     * @param node current node
     * @param entry entry to be inserted
     * @param splitChild key-node pair which points to split child, null if child was not split
     * @return a KeyNode of either the new root or the split child if current node was split, otherwise null
     */
    public KeyNode insertInternal(int key, RecordPointer pointer) {
    	KeyNode splitChild = null;
    	boolean split = false;
        if (this instanceof InternalNode) {
            InternalNode curNode = (InternalNode) this;

            // Find index of pointer to leftmost node that can be inserted with the entry
            int child = curNode.findIndexOfNode(key);

            // Insert entry to subtree
            splitChild = curNode.getPointers()[child].insertInternal(key, pointer);

            if (splitChild != null) {
            	splitChild.getNode().setParent(curNode);
                if (curNode.getDegree() < getN()+1) {
                    // Insert entry to node if it is not full
                    curNode.addSorted(splitChild.getKey(), splitChild.getNode());
                    // No nodes were split after insertion
                    return null;
                } else {
                    // Split node if it is full
                	splitChild = curNode.splitNode(splitChild);
                    split = true;
                }
            }
        } else if (this instanceof LeafNode) {
            LeafNode leafNode = (LeafNode) this;
            if (leafNode.getDegree() < getN()) {
                // Add entry to leaf node if it is not full
                leafNode.addSorted(key, pointer);
                // No nodes were split after insertion
                return null;
            } else {
            	// Check for pre-existing overflow leaf nodes
            	LeafNode rightSibling = leafNode.getRightSibling();
            	while (rightSibling != null && rightSibling.getParent() == null) {
            		if (rightSibling.getDegree() < getN()) {
            			// Add entry to overflow leaf node if it is not full
            			rightSibling.addSorted(key, pointer);
            			// No nodes were split after insertion
            			return null;
            		}
            		rightSibling = rightSibling.getRightSibling();
            	}
                // Split leaf if it is full (overflow leaf nodes which are not pointed to by internal nodes may be created)
                splitChild = leafNode.splitLeaf(key, pointer);
                if (splitChild != null) {
                	split = true;
                }
            }
        }

        if (split && this.isRoot()) {
            // If root is split, add a new node to be the root
            InternalNode newNode = new InternalNode(true);
            newNode.addPointer(this, newNode.getDegree());
            newNode.addSorted(splitChild.getKey(), splitChild.getNode());
            setParent(newNode);
            splitChild.getNode().setParent(newNode);
            //TODO: finish debugging and delete this
            if(getHeight() != splitChild.getNode().getHeight()) {
            	System.out.println("Height tracking fked up somewhere");
            }
            newNode.setHeight(getHeight()+1);
            return new KeyNode(0, newNode);
        }
        return splitChild;
    }

    //TODO: complete refactoring
    /**
     * Delete all entries having the specified key as its primary key
     * @param key key to delete
     */
    public static void delete(Node root, int key) {
        // Initialize total number of nodes deleted for experiment
        int totalNodesDeleted = 0;
        if (root == null) {
            return;
        }
        
        // Keep deleting until the key is not found in the B+ tree
        DeleteResult result;
        do {
            result = root.deleteInternal(key);
            root = result.getParentNode();
        } while (result != null && result.isFound());
    }

    //TODO: rename deleteInternal to bPlusDelete
    /**
     * Internal implementation of deletion in B+ tree
     * 
     * @param key key to delete
     * @return result object consisting of index of deleted child node, parent of
     *         traversed node,
     *         and boolean indicating if an entry is deleted
     */
    public DeleteResult deleteInternal(int key) {
        DeleteResult result;
        Integer oldChildIndex = null;
        InternalNode parentNode = this.getParent();
        boolean found = false;

        if (this instanceof InternalNode) {
            InternalNode node = (InternalNode) this;

            // Find pointer to node possibly containing key to delete
            int childIndex = node.findIndexOfNode(key);

            // Recursively delete
            result = node.getPointers()[childIndex].deleteInternal(key);

            // Retrieve index of deleted child node, null if no deletion
            oldChildIndex = result.getOldChildIndex();

            found = result.isFound();

            if (found && oldChildIndex != null) {
                // Delete key and pointer to deleted child node
                node.deleteKey(oldChildIndex - 1);
                node.deletePointer(oldChildIndex);

                if (node.getDegree() >= (int) Math.floor(getN()/2.0)+1 || node.isRoot()) {
                    oldChildIndex = null;
                } else {
                    // Find index of pointer to current node in parent
                    int curIndex = findIndexOfPointer(parentNode, node);

                    // Get left and right siblings of current node
                    InternalNode rightSibling = null, leftSibling = null;
                    if (curIndex > 0) {
                        leftSibling = (InternalNode) parentNode.getPointers()[curIndex - 1];
                    }
                    if (curIndex < getN()) {
                        rightSibling = (InternalNode) parentNode.getPointers()[curIndex + 1];
                    }

                    if (rightSibling != null && rightSibling.getDegree() > (int) Math.floor(getN()/2.0)+1) {
                        // If right sibling has extra entries, borrow from right sibling
                        // Move first key and pointer of right sibling to current node
                        moveEntryToLeftInternalNode(node, rightSibling);

                        // Swap moved key value with parent key value
                        int temp = node.getKeys()[node.getDegree() - 2];
                        node.getKeys()[node.getDegree() - 2] = parentNode.getKeys()[curIndex];
                        parentNode.getKeys()[curIndex] = temp;
                        oldChildIndex = null;
                    } else if (leftSibling != null && leftSibling.getDegree() > (int) Math.floor(getN()/2.0)+1) {
                        // If left sibling has extra entries, borrow from left sibling
                        // Move last key and pointer of left sibling to current node
                        moveEntryToRightInternalNode(leftSibling, node);

                        // Swap moved key value with parent key value
                        int temp = node.getKeys()[0];
                        node.getKeys()[0] = parentNode.getKeys()[curIndex - 1];
                        parentNode.getKeys()[curIndex - 1] = temp;
                        oldChildIndex = null;
                    } else if (rightSibling != null) {
                        // If right sibling does not have extra entries, merge with right sibling
                        // Set right sibling node to be removed
                        oldChildIndex = curIndex + 1;

                        // Pull down key from parent to current node
                        node.addKey(parentNode.getKeys()[curIndex], node.getDegree() - 1);

                        // Merge right sibling node to current node
                        merge(rightSibling, node);
                    } else if (leftSibling != null) {
                        // If left sibling does not have extra entries, merge with left sibling
                        // Set current node to be removed
                        oldChildIndex = curIndex;

                        // Pull down key from parent to left node
                        leftSibling.addKey(parentNode.getKeys()[curIndex - 1], leftSibling.getDegree() - 1);

                        // Merge current node to left sibling node
                        merge(node, leftSibling);
                    }
                }
            }

            // If root only has 1 child, replace root with child
            if (node.isRoot() && node.getDegree() == 1) {
                Node newRoot = node.getPointers()[0];
                newRoot.setParent(null);
                node.deleteAll();
                //TODO: Increase total number of deleted nodes
                //totalNodesDeleted++;
                //TODO: return newRoot as the new root
                return new DeleteResult(oldChildIndex, newRoot, found);
            }
        } else if (this instanceof LeafNode) {
            LeafNode node = (LeafNode) this;

            RecordPointer deletedPointer = node.delete(key);
            // Delete entry in storage
            if (deletedPointer != null) {
                found = true;
                storage.deleteRecord(deletedPointer);
            }

            if (found) {
            	// Check if node has attached overflow leaf nodes
            	LeafNode rightSibling = node.getRightSibling();
                if(rightSibling != null && rightSibling.getParent() == null) {
                	// Shift entry from overflow leaf node to current node
                	int overflowKey = rightSibling.getKeys()[0];
                	RecordPointer entry = rightSibling.deleteByIndex(0);
                    node.addSorted(overflowKey, entry);
                    if(rightSibling.getDegree() == 0) {
                    	// Delete empty overflow leaf node
                    	node.setRightSibling(rightSibling.getRightSibling());
                    	totalNodesDeleted++;
                    }
                    //TODO: return wtv necessary
                    return new DeleteResult(oldChildIndex, parentNode, found);
                }
                
                // Check if node is under capacity
                if (!node.isRoot() && node.getDegree() < (int) Math.floor(getN()+1/2.0) && !node.isRoot()) {
                    boolean borrowed = false;
                    // Find index of pointer to current node in its parent
                    int curIndex = findIndexOfPointer(parentNode, node);
                    
                    LeafNode leftSibling = null;
                    if (curIndex > 0) {
                    	leftSibling = (LeafNode) parentNode.getPointers()[curIndex-1];
                    }

                    //TODO: if smallest key is deleted how to update parent node???? use found & oldChildIndex
                    // Attempt to borrow keys
                    // Check if the right sibling is a sibling node
                    if (rightSibling != null && rightSibling.getParent() == node.getParent()) {
                        // Check if we can borrow from right sibling
                    	if (rightSibling.getDegree() > (int) Math.floor(getN()+1/2.0)) {
                    		// Get count of smallest key in right sibling
                    		int count = 0;
                    		while (count+1 < rightSibling.getDegree()) {
                    			if (rightSibling.getKeys()[count] == rightSibling.getKeys()[count+1]) {
                    				count++;
                    			} else {
                    				break;
                    			}
                    		}
                    		count++;
                    		
                    		// Check if smallest key in right sibling node can be shifted into current node without worsening balance
                    		if (count < getN()-node.getDegree() && rightSibling.getDegree()-count >= (int) Math.floor(getN()+1/2.0)) {
                    			// Shift those entries into the current node
                    			for (int i = 0; i < count; i++) {
                    				int siblingKey = rightSibling.getKeys()[0];
                    				RecordPointer entry = rightSibling.deleteByIndex(0);
                                    node.addSorted(siblingKey, entry);
                    			}

                    			// Set leftmost key of right sibling to be key value of parent
                    			parentNode.getKeys()[curIndex] = rightSibling.getKeys()[0];
                    			borrowed = true;
                    		}
                    	}
                    }
                    // Check if the left sibling is a sibling node
                    if (!borrowed && leftSibling != null && leftSibling.getRightSibling() == node) {
                    	// Check if we can borrow from left sibling
                        if (leftSibling.getDegree() > (int) Math.floor(getN()+1/2.0)) {
                        	// Get count of largest key in left sibling
                        	int count = 0;
                       		while (count+1 < leftSibling.getDegree()) {
                       			if (leftSibling.getKeys()[leftSibling.getDegree()-count-1] == leftSibling.getKeys()[leftSibling.getDegree()-count-2]) {
                        			count++;
                        		} else {
                        			break;
                        		}
                       		}
                       		count++;

                        	// Check if largest key in left sibling node can be shifted into current node without worsening balance
                        	if (count < getN()-node.getDegree() && leftSibling.getDegree()-count >= (int) Math.floor(getN()+1/2.0)) {
                       			// Shift those entries into the current node
                       			for (int i = 0; i < count; i++) {
                       				int siblingKey = leftSibling.getKeys()[leftSibling.getDegree()-1];
                       				RecordPointer entry = leftSibling.deleteByIndex(leftSibling.getDegree()-1);
                                       node.addSorted(siblingKey, entry);
                       			}

                        		// Set leftmost key of right sibling to be key value of parent
                        		parentNode.getKeys()[curIndex-1] = node.getKeys()[0];
                        		borrowed = true;
                       		}
                       	}
                	}
                    // TODO: Propagate key changes upwards
                    if (borrowed) {
                    	
                    }
                    // Attempt to merge nodes if borrowing is not possible
                    if(!borrowed) {
                    	//TODO: merge with either right or left sibling if merger is possible
                    	// Attempt merge with right sibling
                    	// Check if the right sibling is a sibling node
                    	if (rightSibling != null && rightSibling.getParent() == node.getParent()) {
                    		// Check that right sibling can fit into current node
                    		if (rightSibling.getDegree() <= (int) Math.floor(getN()+1/2.0)) {
                    			oldChildIndex = curIndex+1;
                    			node.merge(rightSibling);
                                // Modify siblings of nodes
                                node.setRightSibling(rightSibling.getRightSibling());
                    		}
                    	}
                    	// Attempt merge with left sibling
                    	// Check if the left sibling is a sibling node
                    	if (leftSibling != null && leftSibling.getRightSibling() == node) {
                    		// Check that current node can fit into left sibling
                    		if (leftSibling.getDegree() <= (int) Math.floor(getN()+1/2.0)) {
                    			oldChildIndex = curIndex;
                    			leftSibling.merge(node);
                                // Modify siblings of nodes
                    			leftSibling.setRightSibling(node.getRightSibling());
                    		}
                    	}
                    }
                }
            }

            // If root has no records delete the empty tree by returning null
            if (node.isRoot() && node.getDegree() == 0) {
            	//TODO: return null for the root to indicate deleted tree
            }
        }

        // Return object consisting of index of deleted child (if any), parent of current node,
        // and a boolean indicating whether an entry is deleted
        return new DeleteResult(oldChildIndex, parentNode, found);
    }

    //TODO: Shift to internalnode class, might have to edit as well
    /**
     * Merge 2 internal nodes by appending keys and pointers of source node to
     * destination node
     * 
     * @param src source internal node
     * @param dst destination internal node, used to store the merged node
     */
    public void merge(InternalNode src, InternalNode dst) {
        // Change parent of source node's child nodes to destination node
        for (int i = 0; i < src.getDegree(); ++i) {
            src.getPointers()[i].setParent(dst);
        }

        // Copy all keys and pointers of the source node to the back of the destination
        // node
        System.arraycopy(src.getKeys(), 0, dst.getKeys(), dst.getDegree(), src.getDegree() - 1);
        System.arraycopy(src.getPointers(), 0, dst.getPointers(), dst.getDegree(), src.getDegree());
        dst.setDegree(src.getDegree() + dst.getDegree());

        // Delete source node
        src.setParent(null);
        src.deleteAll();

        // Increase number of nodes deleted
        totalNodesDeleted++;
    }

    /**
     * Move the leftmost key and pointer of right internal node to left internal
     * node
     * @param left left internal node (destination)
     * @param right right internal node (source)
     */
    public void moveEntryToLeftInternalNode(InternalNode left, InternalNode right) {
        // Delete the first key and pointer of the node on the right
        int key = right.deleteKey(0);
        Node pointer = right.deletePointer(0);

        // Add the key and pointer to the back of the node on the left
        left.addKey(key, left.getDegree() - 1);
        left.addPointer(pointer, left.getDegree());
        pointer.setParent(left);
    }

    /**
     * Move the rightmost key and pointer of left internal node to right internal
     * node
     * @param left left internal node (source)
     * @param right right internal node (destination)
     */
    public void moveEntryToRightInternalNode(InternalNode left, InternalNode right) {
        // Delete the last key and pointer of the node on the left
        int key = left.deleteKey(left.getDegree() - 2);
        Node pointer = left.deletePointer(left.getDegree() - 1);

        // Add the key and pointer to the start of the node on the right
        right.addKey(key, 0);
        right.addPointer(pointer, 0);
        pointer.setParent(right);
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
     * @param index position of the child node in the entire tree (0 for root and all of the subsequent leftmost child nodes)
     * @return total number of nodes from the subtree (inclusive of root)
     */
    public static int getTotalNodes(Node root, int index) {
    	if(root instanceof LeafNode) {
    		return 0;
    	}
    	int total = 0;
    	InternalNode root1 = (InternalNode) root;
    	for(int i = 0; i < root1.getDegree(); i++) {
    		total += getTotalNodes(root1.getPointers()[i], i+index);
    	}
    	
    	//If this is the leftmost internal node with a height of 1, we need to iterate through the leaf nodes to count them all
    	if(total == 0 && index == 0) {
    		LeafNode temp = (LeafNode) root1.getPointers()[0];
    		while(temp != null) {
    			total++;
    			temp = temp.getRightSibling();
    		}
    	}
    	return total+1;
    }

    public void fixTree() {
        if(this == null || this instanceof LeafNode) {
            return;
        }
        else if (this instanceof InternalNode) {
            InternalNode internalNode = (InternalNode) this;

            for (int i = 1; i < internalNode.getDegree(); i++) { // Skip the first pointer
                Node rightNode = internalNode.getPointers()[i];
                replaceKey(internalNode.getKeys()[i-1], rightNode.getLowestKey());
                rightNode.fixTree();
            }
        }
    }

    // Pass in internal, non-root nodes
    public int getLowestKey() {
        if (this instanceof LeafNode) {
            LeafNode leafNode = (LeafNode) this;
            return leafNode.getKeys()[0];
        } else if (this instanceof InternalNode) {
            InternalNode node = (InternalNode) this;
            return node.getPointers()[0].getLowestKey();
        }
        return -1;
    }

    public static Storage getStorage() {
    	return Node.storage;
    }

    public static void setStorage(Storage storage) {
    	Node.storage = storage;
    }

	public static int getN() {
		return Node.n;
	}
	
	//TODO: Figure out actual value of N
    /**
     * Set n parameter of B+ tree from block size
     * @param blockSize size of block in bytes
     */
    public static void setNFromBlockSize(int blockSize) {
        Node.n = (blockSize - 4) / 18;
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

    public void replaceKey(int oldKey, int newKey) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == oldKey) {
                keys[i] = newKey;
                return;
            }
        }
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