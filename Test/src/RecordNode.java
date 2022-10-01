import java.util.ArrayList;
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
     * @return new head of linked list
     */
    public RecordNode addPointer(RecordPointer pointer) {
    	if (this.size < maxSize) {
    		// Insert into current linked list
    		this.pointers[this.size] = pointer;
        	this.size++;
        	return this;
    	} else {
    		// Create a new linked list node as the head of the linked list
    		RecordPointer[] newPointers = new RecordPointer[maxSize];
    		newPointers[0] = pointer;
    		RecordNode newHead = new RecordNode(1, newPointers, this);
    		return newHead;
    	}
    }
    
    /**
     * Retrieve all record pointers from the linked list
     * @param pos index of the record pointer to be retrieved
     * @return record pointer
     */
    public ArrayList<RecordPointer> retrievePointers() {
    	ArrayList<RecordPointer> pointers = new ArrayList<RecordPointer>();
    	RecordNode cur = this;
    	while (cur != null) {
    		for(int i = 0; i < cur.getSize(); i++) {
    			pointers.add(cur.getPointers()[i]);
    		}
    		cur = cur.getNext();
    	}
    	
    	return pointers;
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

    // TODO: set formula
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
