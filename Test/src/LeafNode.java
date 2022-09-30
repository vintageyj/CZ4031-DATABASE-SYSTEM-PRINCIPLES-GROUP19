import java.util.Arrays;

/**
 * Class representing a leaf node in a B+ tree
 * Extends Node abstract class
 */
public class LeafNode extends Node {

    /**
     * Array of pointers to records
     */
    private RecordPointer[] pointers;

    /**
     * Right sibling of the leaf node
     */
    private LeafNode rightSibling;

    /**
     * Storage to keep track of deleted nodes
     */
    private Storage storage;

    /**
     * Construct an empty leaf node specified with whether it is a root node
     * 
     * @param isRoot whether the node is a root node
     */
    public LeafNode(boolean isRoot) {
        this(0, isRoot, new int[getN()], new RecordPointer[getN()], null, null);
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
    public LeafNode(int degree, boolean isRoot, int[] keys, RecordPointer[] pointers) {
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
    public LeafNode(int degree, boolean isRoot, int[] keys, RecordPointer[] pointers, InternalNode parent,
            LeafNode rightSibling) {
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
        RecordPointer[] pointers = getPointers();

        // Temporarily update arrays to store the existing and to be added entry
        setKeys(Arrays.copyOf(keys, keys.length + 1));
        setPointers(Arrays.copyOf(pointers, pointers.length + 1));

        // Find on which index the key and pointer can be inserted to arrays in order to
        // keep it sorted
        int indexToInsert = findIndexToInsert(key);

        // Insert key and pointer
        insertAndShift(key, indexToInsert);
        insertAndShift(entry, indexToInsert);

        // Find point to split node by checking for distinct keys
        int mid = -1;
        int mid2 = (int) Math.ceil((getN() + 1) / 2.0); // This way mid2 will always exceed the range of valid indices
                                                        // first
        int mid1 = mid2 - 1;
        while (mid1 >= 0 || mid2 < getKeys().length) {
            if (mid2 < getKeys().length && getKeys()[mid2] != getKeys()[mid2 - 1]) {
                mid = mid2;
                break;
            } else if (getKeys()[mid1] != getKeys()[mid1 + 1]) {
                mid = mid1;
                break;
            }
            mid2++;
            mid1--;
        }

        if (mid == -1) {
            // All the keys have been checked, and no distinct keys exist
            // Create overflow node
            int[] firstHalfKeys = Arrays.copyOfRange(getKeys(), 0, keys.length);
            RecordPointer[] firstHalfPointers = Arrays.copyOfRange(getPointers(), 0, keys.length);
            int[] secondHalfKeys = Arrays.copyOfRange(getKeys(), keys.length, getKeys().length);
            RecordPointer[] secondHalfPointers = Arrays.copyOfRange(getPointers(), keys.length, getPointers().length);

            // Set key-value pairs to nodes
            setKeys(Arrays.copyOf(firstHalfKeys, keys.length));
            setPointers(Arrays.copyOf(firstHalfPointers, pointers.length));
            setDegree(firstHalfPointers.length);

            // Create a new node to store the split key-value pairs
            LeafNode overflowLeaf = new LeafNode(secondHalfPointers.length, false,
                    Arrays.copyOf(secondHalfKeys, keys.length),
                    Arrays.copyOf(secondHalfPointers, pointers.length));

            // Modify sibling relations on leaf nodes
            LeafNode rightSibling = getRightSibling();
            setRightSibling(overflowLeaf);
            overflowLeaf.setRightSibling(rightSibling);
            return null;
        } else {
            // Split key and pointer arrays into half
            int[] firstHalfKeys = Arrays.copyOfRange(getKeys(), 0, mid);
            RecordPointer[] firstHalfPointers = Arrays.copyOfRange(getPointers(), 0, mid);
            int[] secondHalfKeys = Arrays.copyOfRange(getKeys(), mid, getKeys().length);
            RecordPointer[] secondHalfPointers = Arrays.copyOfRange(getPointers(), mid, getPointers().length);

            // Set key-value pairs to nodes
            setKeys(Arrays.copyOf(firstHalfKeys, keys.length));
            setPointers(Arrays.copyOf(firstHalfPointers, pointers.length));
            setDegree(firstHalfPointers.length);

            // Create a new node to store the split key-value pairs
            LeafNode newLeaf = new LeafNode(secondHalfPointers.length, false,
                    Arrays.copyOf(secondHalfKeys, keys.length),
                    Arrays.copyOf(secondHalfPointers, pointers.length));

            // Modify sibling relations on leaf nodes
            LeafNode rightSibling = getRightSibling();
            setRightSibling(newLeaf);
            newLeaf.setRightSibling(rightSibling);

            // Return pair of the smallest key in second node and pointer to second node
            return new KeyNode(newLeaf.getKeys()[0], newLeaf);
        }
    }

    /**
     * Insert a record pointer to a specific index in the array of pointers, shift
     * the pointers affected by the insertion
     * and delete last pointer in the array
     * 
     * @param pointer record pointer to be inserted
     * @param pos     index to insert
     */
    public void insertAndShift(RecordPointer pointer, int pos) {
        for (int i = pointers.length - 1; i > pos; i--) {
            pointers[i] = pointers[i - 1];
        }
        pointers[pos] = pointer;
    }

    /**
     * Delete a record pointer on the specified index in the array of pointers, then
     * shift the pointers accordingly
     * 
     * @param pos position of pointer to be deleted
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
        insertAndShift(key, index);
        insertAndShift(pointer, index);
        setDegree(getDegree() + 1);
    }

    /**
     * Delete a record that matches the key value
     * 
     * @param key key to delete
     * @return the deleted record's pointer if found, otherwise null
     */
    public RecordPointer delete(int key) {
        for (int i = 0; i < getDegree(); i++) {
            if (getKeys()[i] == key) {
                RecordPointer pointer = pointers[i];
                super.deleteAndShift(i);
                deleteAndShift(i);
                setDegree(getDegree() - 1);
                return pointer;
            }
        }
        return null;
    }

    /**
     * Delete an entry by its index in the node
     * 
     * @param index index of entry to be deleted
     * @return deleted entry
     */
    public RecordPointer deleteByIndex(int index) {
        RecordPointer pointer = pointers[index];
        super.deleteAndShift(index);
        deleteAndShift(index);
        setDegree(getDegree() - 1);
        return pointer;
    }

    /**
     * Delete all entries in the node
     */
    public void deleteAll() {
        Arrays.fill(pointers, null);
        Arrays.fill(getKeys(), 0);
        setDegree(0);

        // Increase number of nodes deleted
        // Calls the logDeletedNodeCount() in storage to update the count of deleted
        // nodes
        storage.logDeletedNodeCount();
    }

    public RecordPointer[] getPointers() {
        return pointers;
    }

    public void setPointers(RecordPointer[] pointers) {
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
        sb.append("[");
        for (int i = 0; i < getDegree(); i++) {
            sb.append(getKeys()[i]);
            sb.append(", ");
        }
        sb.replace(sb.length() - 2, sb.length() - 1, "]");
        return sb.toString();
    }
}