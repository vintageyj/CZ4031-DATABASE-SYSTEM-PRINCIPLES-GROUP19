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
     * Construct an empty internal node of height 0 specified with whether it is a root node
     * 
     * @param isRoot whether the node is a root node
     */
    public InternalNode(boolean isRoot) {
        this(0, 0, isRoot, new int[getN()], new Node[getN() + 1], null);
    }

    /**
     * Construct an internal node with current degree, whether it is a root node,
     * array of keys and pointers
     * 
     * @param degree   current degree of node
     * @param isRoot   whether the node is a root node
     * @param keys     array of keys
     * @param pointers array of pointers
     */
    public InternalNode(int height, int degree, boolean isRoot, int[] keys, Node[] pointers) {
        this(height, degree, isRoot, keys, pointers, null);
    }

    /**
     * Construct an internal node with current degree, whether it is a root node,
     * array of keys and pointers, and parent node
     * 
     * @param degree   current degree of node
     * @param isRoot   whether the node is a root node
     * @param keys     array of keys
     * @param pointers array of pointers
     * @param parent   parent node
     */
    public InternalNode(int height, int degree, boolean isRoot, int[] keys, Node[] pointers, InternalNode parent) {
        super(height, degree, isRoot, keys, parent);
        this.pointers = pointers;
    }

    /**
     * Merge 2 internal nodes by appending keys and pointers of source node to
     * destination node
     * 
     * @param src source internal node
     */
    public void merge(InternalNode src) {
        // Change parent of source node's child nodes to destination node
        for (int i = 0; i < src.getDegree(); ++i) {
            src.getPointers()[i].setParent(this);
        }

        // Copy all keys and pointers of the source node to the back of the destination
        // node
        System.arraycopy(src.getKeys(), 0, getKeys(), getDegree(), src.getDegree() - 1);
        System.arraycopy(src.getPointers(), 0, getPointers(), getDegree(), src.getDegree());
        setDegree(src.getDegree() + getDegree());

        // Delete source node
        src.setParent(null);
        src.deleteAll();

    }


    /**
     * Move the leftmost key and pointer of right internal node to left internal
     * node
     * 
     * @param right right internal node (source)
     */
    public void moveEntryFromRightInternalNode(InternalNode right) {
        // Delete the first key and pointer of the node on the right
        int key = right.deleteKey(0);
        Node pointer = right.deletePointer(0);

        // Add the key and pointer to the back of the node on the left
        this.addKey(key, this.getDegree() - 1);
        this.addPointer(pointer, this.getDegree());
        pointer.setParent(this);
    }

    /**
     * Move the rightmost key and pointer of left internal node to right internal
     * node
     * 
     * @param left  left internal node (source)
     */
    public void moveEntryFromLeftInternalNode(InternalNode left) {
        // Delete the last key and pointer of the node on the left
        int key = left.deleteKey(left.getDegree() - 2);
        Node pointer = left.deletePointer(left.getDegree() - 1);

        // Add the key and pointer to the start of the node on the right
        this.addKey(key, 0);
        this.addPointer(pointer, 0);
        pointer.setParent(this);
    }

    /**
     * Split full internal node into two parts
     * 
     * @param newKeyPointer key and pointer to be added
     * @return the smallest key in the split off node and pointer to that node
     */
    public KeyNode splitNode(KeyNode newKeyPointer) {
        int[] keys = getKeys();
        Node[] pointers = getPointers();

        // Temporarily update arrays to store existing and to be added keys and pointers
        setKeys(Arrays.copyOf(keys, keys.length + 1));
        setPointers(Arrays.copyOf(pointers, pointers.length + 1));

        // Find on which index the key and pointer can be inserted in order to keep it
        // sorted
        int indexToInsertKey = findIndexToInsert(newKeyPointer.getKey());

        // Insert key and pointer to temporary arrays
        insertAndShift(newKeyPointer.getKey(), indexToInsertKey);
        insertAndShift(newKeyPointer.getNode(), indexToInsertKey + 1);

        // Find midpoint to split node, with first half having the extra pointer if
        // relevant
        int mid = (int) Math.ceil(getN() / 2.0);

        // Split key and pointer arrays in half
        int[] firstHalfKeys = Arrays.copyOfRange(getKeys(), 0, mid);
        Node[] firstHalfPointers = Arrays.copyOfRange(getPointers(), 0, mid + 1);
        int[] secondHalfKeys = Arrays.copyOfRange(getKeys(), mid + 1, getKeys().length);
        Node[] secondHalfPointers = Arrays.copyOfRange(getPointers(), mid + 1, getPointers().length);

        // Set keys and pointers to nodes
        setKeys(Arrays.copyOf(firstHalfKeys, keys.length));
        setPointers(Arrays.copyOf(firstHalfPointers, pointers.length));
        setDegree(firstHalfPointers.length);

        // Create a new node to store the split keys and pointers
        InternalNode newNode = new InternalNode(getHeight(), secondHalfPointers.length, false,
                Arrays.copyOf(secondHalfKeys, keys.length),
                Arrays.copyOf(secondHalfPointers, pointers.length));

        // Set the new node as parent of moved nodes
        for (int i = 0; i < newNode.getDegree(); i++) {
            newNode.getPointers()[i].setParent(newNode);
        }

        // Return pair of the smallest key in second node and pointer to second node
        return new KeyNode(keys[mid], newNode);
    }

    /**
     * Find index of child node based on key value provided
     * 
     * @param key value of key
     * @return index of child node
     */
    public int findIndexOfNode(int key) {
        for (int i = 0; i < getDegree()-2; i++) {
            if (key < getKeys()[i]) {
                return i;
            }
        }
        return getDegree()-1;
    }

    /**
     * Insert a node pointer to a specific index in the array of pointers, shift the
     * pointers affected by the insertion
     * and delete last pointer in the array
     * 
     * @param pointer node pointer to be inserted
     * @param pos     index to insert
     */
    public void insertAndShift(Node pointer, int pos) {
        for (int i = pointers.length - 1; i > pos; i--) {
            pointers[i] = pointers[i - 1];
        }
        pointers[pos] = pointer;
    }

    /**
     * Delete a node pointer on the specified index in the array of pointers, then
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
     * Insert a key and node pointer to the node while keeping the key and pointer
     * arrays sorted
     * 
     * @param key     key to be inserted
     * @param pointer node pointer to be inserted
     */
    public void addSorted(int key, Node pointer) {
        int index = findIndexToInsert(key);
        insertAndShift(key, index);
        insertAndShift(pointer, index + 1);
        setDegree(getDegree() + 1);
    }

    /**
     * Add a key with a specified position in the node
     * 
     * @param key key to be added
     * @param pos position to add the key
     */
    public void addKey(int key, int pos) {
        insertAndShift(key, pos);
    }

    /**
     * Add a pointer with a specified position in the node
     * 
     * @param pointer pointer to be added
     * @param pos     position to add the pointer
     */
    public void addPointer(Node pointer, int pos) {
        insertAndShift(pointer, pos);
        setDegree(getDegree() + 1);
    }

    /**
     * Delete a key at the specified position
     * 
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
     * 
     * @param pos position of pointer to be deleted
     * @return deleted pointer
     */
    public Node deletePointer(int pos) {
        Node pointer = pointers[pos];
        deleteAndShift(pos);
        setDegree(getDegree() - 1);
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
        sb.append("Internal Node: ");
        sb.append("[");
        for (int i = 0; i < getDegree()-1; i++) {
            sb.append(getKeys()[i]);
            sb.append(", ");
        }
        sb.replace(sb.length() - 2, sb.length() - 1, "]");
        return sb.toString();
    }
}