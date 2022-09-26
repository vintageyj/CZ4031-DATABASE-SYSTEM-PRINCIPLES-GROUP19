import java.util.Arrays;

/**
 * Class representing a leaf node in a B+ tree
 * Implements Node interface
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
     * Construct an empty leaf node specified with maximum number of keys
     * @param isRoot whether the node is a root node
     */
    public LeafNode(boolean isRoot) {
        this(0, isRoot, new int [getN()], new RecordPointer [getN()], null, null);
    }

    /**
     * 
     * Construct a leaf node with current degree and array of key-value pairs
     * @param degree current degree of node
     * @param isRoot whether the node is a root node
     * @param keys array of keys
     * @param recordPointers array of pointers to records
     */
    public LeafNode(int degree, boolean isRoot, int[] keys, RecordPointer[] recordPointers) {
        this(degree, isRoot, keys, recordPointers, null, null);
    }

    /**
     * Construct a leaf node with all attributes
     * @param degree current degree of node
     * @param kvPairs array representing key-value pairs of the node
     * @param parent parent node
     * @param leftSibling left sibling node
     * @param rightSibling right sibling node
     */
    public LeafNode(int degree, boolean isRoot, int[] keys, RecordPointer[] pointers, InternalNode parent, LeafNode rightSibling) {
    	super(0, degree, isRoot, keys, parent);
        this.pointers = pointers;
        this.rightSibling = rightSibling;
    }

    /**
     * Insert a record pointer to a specific index in the array of pointers, shift the pointers affected by the insertion
     * and delete last pointer in the array
     * @param pointer record pointer to be inserted
     * @param pos index to insert
     */
    public void insertAndShift(RecordPointer pointer, int pos) {
        for (int i = pointers.length - 1; i > pos; i--) {
        	pointers[i] = pointers[i-1];
        }
        pointers[pos] = pointer;
    }

    /**
     * Delete a record pointer on the specified index in the array of pointers, then shift the pointers accordingly
     * @param pos position of pointer to be deleted
     */
    public void deleteAndShift(int pos) {
        for (int i = pos; i < pointers.length - 1; ++i) {
        	pointers[i] = pointers[i+1];
        }
        pointers[pointers.length - 1] = null;
    }

    /**
     * Insert entry to leaf node while keeping the key-value pairs sorted
     * @param entry key-value pair to be inserted
     */
    public void addSorted(int key, RecordPointer pointer) {
        int index = findIndexToInsert(key);
        insertAndShift(key, index);
        insertAndShift(pointer, index);
        setDegree(getDegree()+1);
    }

    /**
     * Delete a record that matches the key value
     * @param key key to delete
     * @return the deleted record's pointer if found, otherwise null
     */
    public RecordPointer delete(int key) {
        for (int i = 0; i < getDegree(); i++) {
            if (getKeys()[i] == key) {
            	RecordPointer pointer = pointers[i];
            	super.deleteAndShift(i);
                deleteAndShift(i);
                setDegree(getDegree()-1);
                return pointer;
            }
        }
        return null;
    }

    /**
     * Delete an entry by its index in the node
     * @param index index of entry to be deleted
     * @return deleted entry
     */
    public RecordPointer deleteByIndex(int index) {
    	RecordPointer pointer = pointers[index];
    	super.deleteAndShift(index);
        deleteAndShift(index);
        setDegree(getDegree()-1);
        return pointer;
    }

    /**
     * Delete all entries in the node
     */
    public void deleteAll() {
        Arrays.fill(pointers, null);
        Arrays.fill(getKeys(), 0);
        setDegree(0);
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
        sb.replace(sb.length()-2, sb.length()-1, "]");
        return sb.toString();
    }
}