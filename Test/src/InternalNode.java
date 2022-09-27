import java.util.Arrays;

/**
 * Class representing internal node (non-leaf node) in a B+ tree
 * Extends Node abstract class
 */
public class InternalNode extends Node {

    /**
     * Array of pointers to child node
     */
    private Node[] pointers;

    /**
     * Construct an empty internal node specified with maximum number of keys
     * @param isRoot whether the node is a root node
     */
    public InternalNode(boolean isRoot) {
        this(0, isRoot, new int[getN()], new Node[getN()+1], null);
    }

    /**
     * Construct an internal node with current degree, array of keys and pointers
     * @param degree current degree of node
     * @param isRoot whether the node is a root node
     * @param keys array of keys
     * @param pointers array of pointers
     */
    public InternalNode(int degree, boolean isRoot, int[] keys, Node[] pointers) {
        this(degree, isRoot, keys, pointers, null);
    }

    /**
     * Construct an internal node with current degree, array of keys and pointers, and parent node
     * @param degree current degree of node
     * @param isRoot whether the node is a root node
     * @param keys array of keys
     * @param pointers array of pointers
     * @param parent parent node
     */
    public InternalNode(int degree, boolean isRoot, int[] keys, Node[] pointers, InternalNode parent) {
    	super(0, degree, isRoot, keys, parent);
        this.pointers = pointers;
    }

    //TODO: Helper functions
    /**
     * Find index of child node based on key value provided
     * @param key value of key
     * @return index of child node
     */
    public int findIndexOfNode(int key) {
    	for (int i = 0; i < getKeys().length; i++) {
    		if (key < getKeys()[i]) {
    			return i;
    		}
    	}
        return getKeys().length;
    }

    /**
     * Insert a node pointer to a specific index in the array of pointers, shift the pointers affected by the insertion
     * and delete last pointer in the array
     * @param pointer node pointer to be inserted
     * @param pos index to insert
     */
    public void insertAndShift(Node pointer, int pos) {
        for (int i = pointers.length - 1; i > pos; i--) {
        	pointers[i] = pointers[i-1];
        }
        pointers[pos] = pointer;
    }

    /**
     * Delete a node pointer on the specified index in the array of pointers, then shift the pointers accordingly
     * @param pos position of pointer to be deleted
     */
    public void deleteAndShift(int pos) {
        for (int i = pos; i < pointers.length - 1; ++i) {
        	pointers[i] = pointers[i+1];
        }
        pointers[pointers.length - 1] = null;
    }

    /**
     * Insert a key and node pointer to the node while keeping the key and pointer arrays sorted
     * @param key key to be inserted
     * @param pointer node pointer to be inserted
     */
    public void addSorted(int key, Node pointer) {
        int index = findIndexToInsert(key);
        insertAndShift(key, index);
        insertAndShift(pointer, index+1);
        setDegree(getDegree()+1);
    }

    /**
     * Add a key with a specified position in the node
     * @param key key to be added
     * @param pos position to add the key
     */
    public void addKey(int key, int pos) {
        insertAndShift(key, pos);
    }

    /**
     * Add a pointer with a specified position in the node
     * @param pointer pointer to be added
     * @param pos position to add the pointer
     */
    public void addPointer(Node pointer, int pos) {
        insertAndShift(pointer, pos);
        setDegree(getDegree()+1);
    }

    /**
     * Delete a key at the specified position
     * @param pos position of key to be deleted
     * @return deleted key
     */
    public int deleteKey(int pos) {
        int key = getKeys()[pos];
        super.deleteAndShift(pos);
        return key;
    }

    /**
     * Delete a pointer at the specified position
     * @param pos position of pointer to be deleted
     * @return deleted pointer
     */
    public Node deletePointer(int pos) {
        Node pointer = pointers[pos];
        deleteAndShift(pos);
        setDegree(getDegree()-1);
        return pointer;
    }

    /**
     * Delete all keys and pointers in the node
     */
    public void deleteAll() {
        Arrays.fill(pointers, null);
        Arrays.fill(getKeys(), 0);
        setDegree(0);
    }

    public Node[] getPointers() {
        return pointers;
    }

    public void setPointers(Node[] pointers) {
        this.pointers = pointers;
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