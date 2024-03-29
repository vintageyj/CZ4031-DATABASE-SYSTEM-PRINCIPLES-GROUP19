import java.util.ArrayList;
import java.util.Arrays;

/**
 * Interface representing a node in a B+ tree
 */
public abstract class Node {
    /**
     * Storage for logging purposes
     */
    protected static Storage storage;
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
     * @param height
     * @param degree
     * @param isRoot
     * @param keys
     * @param parent
     */
    public Node(int height, int degree, boolean isRoot, int[] keys, InternalNode parent) {
        this.height = height;
        this.degree = degree;
        this.isRoot = isRoot;
        this.keys = keys;
        this.parent = parent;
    }

    /**
     * Search for records with the specified value
     * 
     * @param key search key (numVotes)
     * @return a list of record addresses with a key value equal to the search key
     */
    public ArrayList<RecordPointer> search(int key) {
        // Reset logs for experiment
        storage.resetLog();

        // Since there could be more than one result for a search key, searching for a
        // single key can be done
        // by using range search, with the search key as both the lower and upper bound
        return bPlusSearch(null, key, key);
    }

    /**
     * Search for records with value within the given lower and upper bounds
     * 
     * @param lower lower bound of the search key, inclusive (numVotes)
     * @param upper upper bound of the search key, inclusive (numVotes)
     * @return a list of record addresses with a key value ranging from the lower to
     *         upper bounds
     */
    public ArrayList<RecordPointer> search(int lower, int upper) {
        // Reset logs for experiment
        storage.resetLog();

        return bPlusSearch(null, lower, upper);
    }

    /**
     * Search for records with value within the given lower and upper bounds
     * within the B+ Tree
     * 
     * @param results list for storing results
     * @param lower   lower bound of search
     * @param upper   upper bound of search
     * @return a list of record addresses with a key value ranging from the lower to
     *         the upper bounds
     */
    public ArrayList<RecordPointer> bPlusSearch(ArrayList<RecordPointer> results, int lower, int upper) {
        if (results == null) {
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
                    // Record node access of the linked list node
                    storage.logNodeAccess(node.getPointers()[i]);
                    results.addAll(node.getPointers()[i].retrievePointers());
                } else if (upper < keys[i]) {
                    return results;
                }
            }
            // Iterate to right sibling of leaf node
            node.getRightSibling().bPlusSearch(results, lower, upper);

        } else if (this instanceof InternalNode) {
            InternalNode node = (InternalNode) this;
            // Record node access
            storage.logNodeAccess(node);

            // Traverse to the leftmost subtree possibly containing the lower bound
            int child = node.findIndexOfNode(lower);
            node.getPointers()[child].bPlusSearch(results, lower, upper);
        }
        return results;
    }

    /**
     * Insert to B+ tree with numVotes as key
     * Value of the entry is the logical address of the record (Block ID, Record ID)
     * 
     * @param root    root node of the B+ Tree
     * @param record  record to be inserted
     * @param pointer address of record to be inserted
     * @return root root node of the B+ tree
     */
    public static Node insert(Node root, Record record, RecordPointer pointer) {
        // Create new tree if root is null
        if (root == null) {
            root = new LeafNode(true);
        }

        int key = record.getNumVotes();
        // Insert by traversing the tree from the root node
        KeyNode newRoot = root.bPlusInsert(key, pointer);
        return newRoot.getNode();
    }

    /**
     * Recursive insertion in B+ tree
     * @param key
     * @param pointer
     * @return a KeyNode of either the new root or the split child if current node
     * was split, otherwise null
     */
    public KeyNode bPlusInsert(int key, RecordPointer pointer) {
        KeyNode splitChild = null;
        boolean split = false;
        if (this instanceof InternalNode) {
            InternalNode curNode = (InternalNode) this;

            // Find index of pointer to leftmost node that can be inserted with the entry
            int child = curNode.findIndexOfNode(key);

            // Insert entry to subtree
            splitChild = curNode.getPointers()[child].bPlusInsert(key, pointer);

            if (splitChild != null) {
                splitChild.getNode().setParent(curNode);
                if (curNode.getDegree() < getN() + 1) {
                    // Insert entry to node if it is not full
                    curNode.addSorted(splitChild.getKey(), splitChild.getNode());
                    // No nodes were split after insertion
                    splitChild = null;
                } else {
                    splitChild = curNode.splitNode(splitChild);
                    split = true;
                }
            }
        } else if (this instanceof LeafNode) {
            LeafNode curNode = (LeafNode) this;
            if (curNode.getDegree() < getN() || Arrays.stream(curNode.getKeys()).anyMatch(i -> i == key)) {
                // Add entry to leaf node if it is not full or if key is already present
                curNode.addSorted(key, pointer);
                // No nodes were split after insertion
                splitChild = null;
            } else {
                // Split leaf if it is full
                splitChild = curNode.splitLeaf(key, pointer);
                if (splitChild != null) {
                    split = true;
                }
            }
        }

        if (this.isRoot()) {
            if (split) {
                // If root is split, add a new node to be the root
                this.setRoot(false);
                InternalNode newNode = new InternalNode(true);
                newNode.setHeight(getHeight() + 1);
                newNode.addPointer(this, newNode.getDegree());
                newNode.addPointer(splitChild.getNode(), newNode.getDegree());
                newNode.addKey(splitChild.getKey(), 0);
                setParent(newNode);
                splitChild.getNode().setParent(newNode);
                return new KeyNode(0, newNode);
            } else {
                // Return the root node to calling method
                return new KeyNode(0, this);
            }
        }
        return splitChild;
    }

    /**
     * Delete all entries having the specified key as its primary key
     * 
     * @param root      root node of the B+ tree
     * @param deleteKey key to delete
     * @return root node of the tree
     */
    public static Node delete(Node root, int deleteKey) {
        // Initialize total number of nodes deleted for experiment
        if (root == null) {
            return null;
        }

        // Delete until the key is not found in the B+ tree
        DeleteResult result;
        result = root.bPlusDelete(deleteKey, null);
        return result.getParentNode();
    }

    /**
     * Internal implementation of deletion in B+ tree
     * 
     * @param key           key to delete
     * @param oldChildIndex index of deleted child node if any, otherwise null
     * @return result object consisting of index of deleted child node, parent of
     *         traversed node,
     *         and boolean indicating if an entry is deleted
     */
    public DeleteResult bPlusDelete(int key, Integer oldChildIndex) {
        DeleteResult result;
        InternalNode parentNode = this.getParent();
        boolean found = false;

        if (this instanceof InternalNode) {
            InternalNode node = (InternalNode) this;

            // Find pointer to node possibly containing key to delete
            int pointerIndex = node.findIndexOfNode(key);

            // Recursively delete
            result = node.getPointers()[pointerIndex].bPlusDelete(key, oldChildIndex);

            // Retrieve index of deleted child node, null if no deletion
            oldChildIndex = result.getOldChildIndex();

            // Set current node since parent of traversed nodes in the lower level may be
            // different after recursion
            node = (InternalNode) result.getParentNode();
            found = result.isFound();

            parentNode = node.getParent();

            if (found && oldChildIndex != null) {
                // Delete key and pointer to deleted child node
                node.deleteKey(oldChildIndex - 1);
                node.deletePointer(oldChildIndex);

                if (node.getDegree() >= (int) Math.floor(getN() / 2.0) + 1 || node.isRoot()) {
                    oldChildIndex = null;
                } else {
                    // Find index of pointer to current node in parent
                    int curNodeIndex = node.findIndexInParent();

                    // Get left and right siblings of current node
                    InternalNode rightSibling = null, leftSibling = null;
                    if (curNodeIndex > 0) {
                        leftSibling = (InternalNode) parentNode.getPointers()[curNodeIndex - 1];
                    }
                    if (curNodeIndex < getN()) {
                        rightSibling = (InternalNode) parentNode.getPointers()[curNodeIndex + 1];
                    }

                    if (rightSibling != null && rightSibling.getDegree() > (int) Math.floor(getN() / 2.0) + 1) {
                        // If right sibling has extra entries, borrow from right sibling
                        // Move first key and pointer of right sibling to current node
                        node.moveEntryFromRightInternalNode(rightSibling);

                        // Swap moved key value with parent key value (first key of right sibling is
                        // larger than any value currently in node)
                        int temp = node.getKeys()[node.getDegree() - 2];
                        node.getKeys()[node.getDegree() - 2] = parentNode.getKeys()[curNodeIndex];
                        parentNode.getKeys()[curNodeIndex] = temp;
                        oldChildIndex = null;
                    } else if (leftSibling != null && leftSibling.getDegree() > (int) Math.floor(getN() / 2.0) + 1) {
                        // If left sibling has extra entries, borrow from left sibling
                        // Move last key and pointer of left sibling to current node
                        node.moveEntryFromLeftInternalNode(leftSibling);

                        // Swap moved key value with parent key value (last key of left sibling is the
                        // smallest value currently in node)
                        int temp = node.getKeys()[0];
                        node.getKeys()[0] = parentNode.getKeys()[curNodeIndex - 1];
                        parentNode.getKeys()[curNodeIndex - 1] = temp;
                        oldChildIndex = null;
                    } else if (rightSibling != null) {
                        // If right sibling does not have extra entries, merge with right sibling
                        // Set right sibling node to be removed
                        oldChildIndex = curNodeIndex + 1;

                        // Pull down key from parent to current node
                        node.addKey(parentNode.getKeys()[curNodeIndex], node.getDegree() - 1);

                        // Merge right sibling node to current node
                        node.merge(rightSibling);
                        // Increase total number of deleted nodes
                        storage.logDeletedNodeCount();
                    } else if (leftSibling != null) {
                        // If left sibling does not have extra entries, merge with left sibling
                        // Set current node to be removed
                        oldChildIndex = curNodeIndex;

                        // Pull down key from parent to left node
                        leftSibling.addKey(parentNode.getKeys()[curNodeIndex - 1], leftSibling.getDegree() - 1);

                        // Merge current node to left sibling node
                        leftSibling.merge(node);
                        // Increase total number of deleted nodes
                        storage.logDeletedNodeCount();
                    }
                }
            }

            // Fix the remaining keys in the node
            if (found) {
                node.fixTree();
            }

            // If current node is root, return root
            if (node.isRoot()) {
                // If root only has 1 child, make child node the new root
                if (node.getDegree() == 1) {
                    InternalNode temp = (InternalNode) node.getPointers()[0];
                    temp.setParent(null);
                    node.deleteAll();

                    // Increase total number of deleted nodes
                    storage.logDeletedNodeCount();
                    return new DeleteResult(null, temp, found);
                }
                return new DeleteResult(null, node, found);
            }

        } else if (this instanceof LeafNode) {
            LeafNode node = (LeafNode) this;

            // Traverse leaf nodes to search for key to delete, since it is possible that
            // the current node
            // does not contain the key
            RecordNode deletedEntry = node.delete(key);
            while (deletedEntry == null && node != null && node.getDegree() > 0
                    && key >= node.getKeys()[node.getDegree() - 1]) {
                node = node.getRightSibling();
                if (node != null)
                    deletedEntry = node.delete(key);
            }

            // Delete entry in storage
            if (deletedEntry != null) {
                found = true;
                while (deletedEntry != null) {
                    for (int i = 0; i < deletedEntry.getSize(); i++) {
                        storage.deleteRecord(deletedEntry.getPointers()[i]);
                    }
                    deletedEntry = deletedEntry.getNext();
                }

            }

            if (found && node.getDegree() < (int) Math.floor((getN() + 1) / 2.0) && !node.isRoot()) {
                // Set parent node since current node may have a different parent after
                // traversing leaf nodes
                parentNode = node.getParent();

                // Find index of pointer to current node in its parent
                int curNodeIndex = node.findIndexInParent();

                // Get left and right siblings of current node, if any
                LeafNode rightSibling = null, leftSibling = null;
                if (curNodeIndex < parentNode.getDegree() - 1) {
                    rightSibling = node.getRightSibling();
                }
                if (curNodeIndex > 0) {
                    leftSibling = (LeafNode) parentNode.getPointers()[curNodeIndex - 1];
                }

                if (rightSibling != null && rightSibling.getDegree() > (int) Math.floor((getN() + 1) / 2.0)) {
                    // Borrow from right sibling
                    int borrowedKey = rightSibling.getKeys()[0];
                    RecordNode entry = rightSibling.deleteByIndex(0);
                    node.addKey(borrowedKey, entry);

                    // Set leftmost key of right sibling to be key value of parent
                    parentNode.getKeys()[curNodeIndex] = rightSibling.getKeys()[0];
                } else if (leftSibling != null && leftSibling.getDegree() > (int) Math.floor((getN() + 1) / 2.0)) {
                    // Borrow from left sibling
                    int borrowedKey = leftSibling.getKeys()[0];
                    RecordNode entry = leftSibling.deleteByIndex(leftSibling.getDegree() - 1);
                    node.addKey(borrowedKey, entry);

                    // Set rightmost key of current node to be key value of parent
                    parentNode.getKeys()[curNodeIndex - 1] = node.getKeys()[0];
                } else if (rightSibling != null) {
                    // Merge with right sibling
                    oldChildIndex = curNodeIndex + 1;
                    node.merge(rightSibling);

                    // Modify siblings of nodes
                    node.setRightSibling(rightSibling.getRightSibling());
                    // Increase total number of deleted nodes
                    storage.logDeletedNodeCount();
                } else if (leftSibling != null) {
                    // Merge with left sibling
                    oldChildIndex = curNodeIndex;
                    leftSibling.merge(node);

                    // Modify siblings of nodes
                    leftSibling.setRightSibling(node.getRightSibling());
                    // Increase total number of deleted nodes
                    storage.logDeletedNodeCount();
                }
            }

            // If current node is root, return root
            if (node.isRoot()) {
                // If root only has no children, return null
                if (node.getDegree() == 0) {
                    // Increase total number of deleted nodes
                    storage.logDeletedNodeCount();
                    return new DeleteResult(oldChildIndex, null, found);
                }
                return new DeleteResult(oldChildIndex, node, found);
            }
        }

        // Return object consisting of index of deleted child (if any), parent of
        // current node,
        // and a boolean indicating whether an entry is deleted
        return new DeleteResult(oldChildIndex, parentNode, found);
    }

    /**
     * Find index of pointer to node in its parent node
     * 
     * @return index of node
     */
    public int findIndexInParent() {
        Node[] pointers = this.getParent().getPointers();
        int i;
        for (i = 0; i < pointers.length; ++i) {
            if (pointers[i] == this)
                break;
        }
        return i;
    }

    /**
     * Find index to insert a new key to the array of keys
     * 
     * @param k key to be inserted
     * @return index to insert
     */
    public int findIndexToInsert(int k) {
        int low = 0, high, mid;
        if (this.getDegree() == 0) {
            return 0;
        }
        if (this instanceof InternalNode) {
            high = this.getDegree() - 2;
        } else {
            high = this.getDegree() - 1;
        }

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
        return k > keys[low] ? low + 1 : low;
    }

    /**
     * Insert a key to a specific index in the array of keys, shift the keys
     * affected by the insertion
     * and delete last key in the array
     * 
     * @param key key to be inserted
     * @param pos index to insert
     */
    public void insertAndShift(int key, int pos) {
        for (int i = keys.length - 1; i > pos; i--) {
            keys[i] = keys[i - 1];
        }
        keys[pos] = key;
    }

    /**
     * Delete a key on the specified index in the array of keys, then shift the keys
     * accordingly
     * 
     * @param pos position of key to be deleted
     */
    public void deleteAndShift(int pos) {
        for (int i = pos; i < keys.length - 1; i++) {
            keys[i] = keys[i + 1];
        }
        keys[keys.length - 1] = 0;
    }

    /**
     * Recursively find the total number of nodes in a B+ tree
     * 
     * @param root root node of the current subtree
     * @return total number of nodes from the subtree (inclusive of root)
     */
    public static int getTotalNodes(Node root) {

        int total = 0;
        if (root instanceof LeafNode) {
            LeafNode root1 = (LeafNode) root;
            // Find total number of linked list nodes
            for (int i = 0; i < root1.getDegree(); i++) {
                RecordNode cur = root1.getPointers()[i];
                while (cur != null) {
                    total++;
                    cur = cur.getNext();
                }
            }
            return total + 1;
        }
        InternalNode root1 = (InternalNode) root;
        for (int i = 0; i < root1.getDegree(); i++) {
            total += getTotalNodes(root1.getPointers()[i]);
        }
        return total + 1;
    }

    /**
     * Fix the keys in the current internal node
     */
    public void fixTree() {
        if (this == null || this instanceof LeafNode) {
            return;
        } else if (this instanceof InternalNode) {
            InternalNode internalNode = (InternalNode) this;

            for (int i = 1; i < internalNode.getDegree(); i++) { // Skip the first pointer
                Node rightNode = internalNode.getPointers()[i];
                replaceKey(internalNode.getKeys()[i - 1], rightNode.getLowestKey());
            }
        }
    }

    /**
     * Find the smallest key in the subtree rooted at the node
     * 
     * @return smallest key in the subtree, or -1 if error occurs
     */
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

    /**
     * Set n parameter of B+ tree from block size
     * 
     * @param blockSize size of block in bytes
     */
    public static void setNFromBlockSize(int blockSize) {
        Node.n = (blockSize - 2 * 4 - 2 * 4) / (4 + 4);
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