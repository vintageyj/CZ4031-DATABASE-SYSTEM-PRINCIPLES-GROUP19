import java.util.Arrays;

/**
 * Class representing a leaf node in a B+ tree
 * Extends Node abstract class
 */
public class LeafNode extends Node {

    /**
     * Array of pointers to linked lists containing record pointers
     */
    private RecordNode[] pointers;

    /**
     * Right sibling of the leaf node
     */
    private LeafNode rightSibling;

    /**
     * Construct an empty leaf node specified with whether it is a root node
     * 
     * @param isRoot whether the node is a root node
     */
    public LeafNode(boolean isRoot) {
        this(0, isRoot, new int [getN()], new RecordNode [getN()], null, null);
    }

    /**
     * 
     * Construct a leaf node with current degree, whether it is a root node, array
     * of keys and record pointers
     * 
     * @param degree   current degree of node
     * @param isRoot   whether the node is a root node
     * @param keys     array of keys
     * @param pointers array of pointers to records
     */
    public LeafNode(int degree, boolean isRoot, int[] keys, RecordNode[] pointers) {
        this(degree, isRoot, keys, pointers, null, null);
    }

    /**
     * 
     * Construct a leaf node with all attributes
     * 
     * @param degree       current degree of node
     * @param isRoot       whether the node is a root node
     * @param keys         array of keys
     * @param pointers     array of pointers to records
     * @param parent       parent node
     * @param rightSibling right sibling node
     */
    public LeafNode(int degree, boolean isRoot, int[] keys, RecordNode[] pointers, InternalNode parent, LeafNode rightSibling) {
    	super(0, degree, isRoot, keys, parent);
        this.pointers = pointers;
        this.rightSibling = rightSibling;
    }

    /**
     * Merge another leaf node into this leaf node by appending key-value pairs of
     * the source node to the calling node
     * 
     * @param src source leaf node
     */
    public void merge(LeafNode src) {
        // Copy all key-value pairs of the source node to the back of the destination
        // node
        System.arraycopy(src.getKeys(), 0, getKeys(), getDegree(), src.getDegree());
        System.arraycopy(src.getPointers(), 0, getPointers(), getDegree(), src.getDegree());
        setDegree(getDegree() + src.getDegree());

        // Delete source node
        src.deleteAll();

    }

    /**
     * Split full leaf node into two parts (one of which may be an overflow leaf
     * node)
     * 
     * @param node  leaf node to be split
     * @param entry entry to be added
     * @return pair of the smallest key in second node and pointer to second node,
     *         or null if an overflow leaf node was created
     */
    public KeyNode splitLeaf(int key, RecordPointer entry) {
        int[] keys = getKeys();
        RecordNode[] pointers = getPointers();

        // Temporarily update arrays to store the existing and to be added entry
        setKeys(Arrays.copyOf(keys, keys.length + 1));
        setPointers(Arrays.copyOf(pointers, pointers.length + 1));

        // Find on which index the key and pointer can be inserted to arrays in order to
        // keep it sorted
        int indexToInsert = findIndexToInsert(key);

        // Insert key and pointer
        insertAndShift(key, indexToInsert);
        insertAndShift(entry, indexToInsert);

        // Find point to split node
        int mid = (int) Math.floor((getN()+1)/2.0);
       
        // Split key and pointer arrays into half
        int[] firstHalfKeys = Arrays.copyOfRange(getKeys(), 0, mid);
        RecordNode[] firstHalfPointers = Arrays.copyOfRange(getPointers(), 0, mid);
        int[] secondHalfKeys = Arrays.copyOfRange(getKeys(), mid, getKeys().length);
        RecordNode[] secondHalfPointers = Arrays.copyOfRange(getPointers(), mid, getPointers().length);

        // Set key-value pairs to nodes
        setKeys(Arrays.copyOf(firstHalfKeys, keys.length));
        setPointers(Arrays.copyOf(firstHalfPointers, pointers.length));
        setDegree(firstHalfPointers.length);

        // Create a new node to store the split key-value pairs
        LeafNode newLeaf = new LeafNode(secondHalfPointers.length, false, Arrays.copyOf(secondHalfKeys, keys.length),
        		Arrays.copyOf(secondHalfPointers, pointers.length));

        // Modify sibling relations on leaf nodes
        LeafNode rightSibling = getRightSibling();
        setRightSibling(newLeaf);
        newLeaf.setRightSibling(rightSibling);

        // Return pair of the smallest key in second node and pointer to second node
        return new KeyNode(newLeaf.getKeys()[0], newLeaf);
    }

    /**
     * Insert a record pointer in a new linked list to a specific index in the array of linked lists, shift the linked lists affected by the insertion and delete last linked list in the array
     * @param pointer record pointer to be inserted
     * @param pos     index to insert
     */
    public void insertAndShift(RecordPointer pointer, int pos) {
        for (int i = pointers.length - 1; i > pos; i--) {
            pointers[i] = pointers[i - 1];
        }
        RecordPointer[] newPointers = new RecordPointer[RecordNode.getMaxSize()];
		newPointers[0] = pointer;
		RecordNode newHead = new RecordNode(1, newPointers, null);
        pointers[pos] = newHead;
    }

    /**
     * Delete a linked list on the specified index in the array of linked lists, then shift the linked lists accordingly
     * @param pos position of linked list to be deleted
     */
    public void deleteAndShift(int pos) {
        for (int i = pos; i < pointers.length - 1; ++i) {
            pointers[i] = pointers[i + 1];
        }
        pointers[pointers.length - 1] = null;
    }

    /**
     * Insert entry to leaf node while keeping the keys and record pointers sorted
     * 
     * @param key     key to be inserted
     * @param pointer record pointer to be inserted
     */
    public void addSorted(int key, RecordPointer pointer) {
        int index = findIndexToInsert(key);
        if(index < getN() && getKeys()[index] == key) {
        	// Insert into the already existing linked list
        	getPointers()[index] = getPointers()[index].addPointer(pointer);
        } else {
        	// Create a new linked list
        	insertAndShift(key, index);
        	insertAndShift(pointer, index);
            setDegree(getDegree()+1);
        }
    }

    /**
     * Insert new linked list to leaf node while keeping the keys and linked lists sorted
     * 
     * @param key     key to be inserted
     * @param pointer linked list to be inserted
     */
    public void addKey(int key, RecordNode pointer) {
        int index = findIndexToInsert(key);
        // Insert the new linked list
        insertAndShift(key, index);
        RecordNode[] pointers = this.getPointers();
        for (int i = pointers.length - 1; i > index; i--) {
            pointers[i] = pointers[i - 1];
        }
        pointers[index] = pointer;
        setDegree(getDegree()+1);
    }

    /**
     * Delete records that match the key value
     * @param key key to delete
     * @return the deleted records' linked list if found, otherwise null
     */
    public RecordNode delete(int key) {
        for (int i = 0; i < getDegree(); i++) {
            if (getKeys()[i] == key) {
            	RecordNode temp, list = pointers[i];
            	super.deleteAndShift(i);
                deleteAndShift(i);
                setDegree(getDegree()-1);
                temp = list;
                // Increase total number of deleted nodes
                while (temp != null) {
                    storage.logDeletedNodeCount();
                    temp = temp.getNext();
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Delete an entry's corresponding linked list by its index in the node
     * @param index index of entry to be deleted
     * @return deleted entry
     */
    public RecordNode deleteByIndex(int index) {
    	RecordNode list = pointers[index];
    	super.deleteAndShift(index);
        deleteAndShift(index);
        setDegree(getDegree()-1);
        return list;
    }

    /**
     * Delete all entries in the node
     */
    public void deleteAll() {
        Arrays.fill(pointers, null);
        Arrays.fill(getKeys(), 0);
        setDegree(0);
    }

    public RecordNode[] getPointers() {
        return pointers;
    }

    public void setPointers(RecordNode[] pointers) {
        this.pointers = pointers;
    }

    public LeafNode getRightSibling() {
        return rightSibling;
    }

    public void setRightSibling(LeafNode rightSibling) {
        this.rightSibling = rightSibling;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Leaf Node: ");
        sb.append("[");
        for (int i = 0; i < getDegree(); i++) {
            sb.append(getKeys()[i]);
            sb.append(", ");
        }
        if (sb.length() > 2) {
            sb.replace(sb.length() - 2, sb.length() - 1, "]");
        } else {
            sb.append("]");
        }
        return sb.toString();
    }
}