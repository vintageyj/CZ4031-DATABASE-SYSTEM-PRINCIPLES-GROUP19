import java.util.Arrays;

/**
 * Class representing a linked list node pointed to by a leaf node of a B Plus Tree.
 * Contains record pointers.
 */
public class RecordNode {

	/**
	 * Maximum number of record pointers that can be stored inside a node
	 */
	private static int maxSize;

	/**
	 * Number of record pointers currently stored inside the node
	 */
	private int size;
	/**
     * Array of pointers to records
     */
    private RecordPointer[] pointers;

    /**
     * Next node of the linked list
     */
    private RecordNode next;
    
    /**
     * Create a linked list node using all the attributes
     * @param size
     * @param pointers
     * @param next
     */
    public RecordNode(int size, RecordPointer[] pointers, RecordNode next) {
    	this.size = size;
    	this.pointers = pointers;
    	this.next = next;
    }
    
    /**
     * Add a record pointer to the linked list node
     * @param pointer record pointer to be added
     */
    public void addPointer(RecordPointer pointer) {
    	this.pointers[this.size] = pointer;
    	this.size++;
    }
    
    /**
     * Retrieve a record pointer from a specific index
     * @param pos index of the record pointer to be retrieved
     * @return record pointer
     */
    public RecordPointer retrievePointer(int pos) {
    	return this.pointers[pos];
    }

    /**
     * Delete the linked list node
     * @return the next linked list node in the linked list (null if linked list is empty)
     */
    public RecordNode deleteNode() {
    	Arrays.fill(pointers, null);
    	return this.next;
    }
    
    /**
     * Get maximum number of record pointers that can be stored in linked list node
     */
    public static int getMaxSize() {
    	return RecordNode.maxSize;
    }

    /**
     * Set maximum number of record pointers that can be stored in linked list node from block size
     * @param blockSize size of block in bytes
     */
    public static void setMaxSizeFromBlockSize(int blockSize) {
    	RecordNode.maxSize = (blockSize-2)/12;
    }
    
    public int getSize() {
    	return this.size;
    }

    public void setSize(int size) {
    	this.size = size;
    }

    public RecordPointer[] getPointers() {
    	return this.pointers;
    }
    
    public void setPointers(RecordPointer[] pointers) {
    	this.pointers = pointers;
    }
    
    public RecordNode getNext() {
    	return this.next;
    }
    
    public void setNext(RecordNode next) {
    	this.next = next;
    }
}
