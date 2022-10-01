import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Storage {
    private final int MEMORY_SIZE;
    private final int BLOCK_SIZE;
    private final int RECORD_SIZE;
    private final int NUM_OF_RECORD;

    private byte[] blocks;
    private int blockTail;
    private LinkedList<RecordPointer> buffer; // changed emptyRecord to buffer? Buffer to store the list of available
                                              // spaces to be populated by KeyPointers

    private BPlusTree bPlusTree;

    // Logging Components
    private List<RecordPointer> accessedBlocks;
    private List<Node> accessedNodes;
    private int blockAccessCount;
    private int nodeAccessCount;
    private final int MAX_ACCESS = 5;
    private int deletedNodeCount;

    public Storage(int blockSize, int recordSize, int memorySize) {
        MEMORY_SIZE = memorySize;
        BLOCK_SIZE = blockSize;
        RECORD_SIZE = recordSize;
        NUM_OF_RECORD = BLOCK_SIZE / RECORD_SIZE;

        blockTail = -1;
        blocks = new byte[MEMORY_SIZE];
        buffer = new LinkedList<>();

        initLogger();
    }

    // Initialise the storage with the given input file in .tsv format
    public void initWithTSV(String path) {
        try {
            // Reads the file line by line
            Reader in = new FileReader(path);
            BufferedReader buf = new BufferedReader(in);
            String line = buf.readLine();
            line = buf.readLine();
            try {
                while (line != null) {
                    // Split and parse the data in each line to create a new record
                    String[] lineItems = line.split("\\s"); // splitting the line and adding its items in String[]
                    createRecord(lineItems[0], Float.parseFloat(lineItems[1]), Integer.parseInt(lineItems[2]));
                    line = buf.readLine();
                }
                buf.close();
            } catch (Exception e) {
                System.out.println("Something went wrong. " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Wrong file path. " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error while reading file. " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build B+ tree on database by inserting the records from database sequentially
     */
    public void buildIndex() {
        bPlusTree = new BPlusTree(Util.getNFromBlockSize(BLOCK_SIZE), this);
        // Iterates through data blocks
        for (int blockID = 0; blockID <= blockTail; ++blockID) {
            Block block = Block.fromByteArray(readBlock(blockID), RECORD_SIZE);
            // Iterates through all record spaces since non-clustered index is used
            for (int recordID = 0; recordID < NUM_OF_RECORD; ++recordID) {
                Record record = block.readRecord(recordID);
                if (!record.isEmpty()) {
                    bPlusTree.insert(record, new RecordPointer(blockID, recordID));
                }
            }
        }
    }

    /**
     * Insert a new record into "disk storage"
     * 
     * @param tConst   data for the record
     * @param rating   data for the record
     * @param numVotes data for the record
     */
    public RecordPointer createRecord(String tConst, float rating, int numVotes) {
        // If the linked list buffer storing the next available KeyPointer addresses is
        // empty, then initialise the linked list buffer
        if (buffer.isEmpty()) {
            createBlock();
        }

        // Retrieves the next available space from the buffer
        RecordPointer address = buffer.remove();

        // Retrieve the block storing the next available space
        Block block = Block.fromByteArray(readBlock(address.getBlockID()), RECORD_SIZE);
        block.updateRecord(address.getRecordID(), tConst, rating, numVotes, false);
        updateBlock(address.getBlockID(), block.toByteArray());

        return address;
    }

    /**
     * Read a record given its address
     * 
     * @param address address of record to get
     */
    public Record readRecord(RecordPointer address) {
        // Reading a record incurs an I/O access to its block
        logBlockAccess(address);
        Block block = Block.fromByteArray(readBlock(address.getBlockID()), RECORD_SIZE);
        Record record = block.readRecord(address.getRecordID());
        return record;
    }

    /**
     * Delete a record given its address, reallocate it for reuse
     * 
     * @param address address of record to be deleted
     */
    public void deleteRecord(RecordPointer address) {
        Block block = Block.fromByteArray(readBlock(address.getBlockID()), RECORD_SIZE);
        block.deleteRecord(address.getRecordID());
        updateBlock(address.getBlockID(), block.toByteArray());

        buffer.add(address);
    }

    /**
     * Prepare a new block so that it can be used.
     */
    public void createBlock() {
        blockTail++;
        Block block = Block.empty(BLOCK_SIZE, RECORD_SIZE);
        updateBlock(blockTail, block.toByteArray());
        for (int recordID = 0; recordID < NUM_OF_RECORD; ++recordID) {
            buffer.add(new RecordPointer(blockTail, recordID));
        }
    }

    /**
     * Update block in "disk storage".
     * 
     * @param blockID ID of block to be updated
     * @param data    data of the updated block
     */
    public void updateBlock(int blockID, byte[] data) {
        System.arraycopy(data, 0, blocks, blockID * BLOCK_SIZE, BLOCK_SIZE);
    }

    /**
     * Get a block from "disk storage"
     * 
     * @param blockID ID of the block to get
     * @return a block with given ID
     */
    // Retrieves the block from disk using block <<blockID>>'s base + offset address
    public byte[] readBlock(int blockID) {
        int baseAddress = blockID * BLOCK_SIZE;
        int offset = baseAddress + BLOCK_SIZE;
        return Arrays.copyOfRange(blocks, baseAddress, offset);
    }

    public BPlusTree getBPT() {
        return bPlusTree;
    }

    /**
     * Search for records given the key (numVotes), using index
     * 
     * @param searchKey search key value
     * @return list of records matching the key value
     */
    public List<Record> searchBPT(int searchKey) {
        List<RecordPointer> recordPointers = bPlusTree.search(searchKey);
        List<Record> records = new LinkedList<>();
        for (RecordPointer ra : recordPointers) {
            records.add(readRecord(ra));
        }
        return records;
    }

    /**
     * Search for records given the lower and upper bounds, using index
     * 
     * @param lower lower bound of search key value, inclusive (numVotes)
     * @param upper upper bound of search key value, inclusive (numVotes)
     * @return list of records having the key value within the lower and upper
     *         bounds
     */
    public List<Record> searchBPT(int lower, int upper) {
        List<RecordPointer> recordPointers = bPlusTree.search(lower, upper);
        List<Record> records = new LinkedList<>();
        for (RecordPointer ra : recordPointers) {
            records.add(readRecord(ra));
        }
        return records;
    }

    /**
     * Delete records that match the key value, using index
     * 
     * @param deleteKey
     */
    public void deleteBPT(int deleteKey) {
        bPlusTree.delete(deleteKey);
    }

    public int getNumBlocksUsed() {
        return blockTail + 1;
    }

    public int getRecordSize() {
        return RECORD_SIZE;
    }

    private void initLogger() {
        accessedBlocks = new LinkedList<>();
        accessedNodes = new LinkedList<>();
        blockAccessCount = 0;
        nodeAccessCount = 0;
        deletedNodeCount = 0;
    }

    public void logDeletedNodeCount() {
        deletedNodeCount++;
    }

    public void logBlockAccess(RecordPointer recordPointer) {
        blockAccessCount++;
        if (accessedBlocks.size() < MAX_ACCESS)
            accessedBlocks.add(recordPointer);
    }

    public void logNodeAccess(Node node) {
        nodeAccessCount++;
        if (accessedNodes.size() < MAX_ACCESS)
            accessedNodes.add(node);
    }

    public void resetLog() {
        blockAccessCount = 0;
        nodeAccessCount = 0;
        deletedNodeCount = 0;
        accessedBlocks.clear();
        accessedNodes.clear();
    }

    public String getBlockLog() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= accessedBlocks.size(); i++) {
            RecordPointer recordPointer = accessedBlocks.get(i);
            byte[] byteArray = readBlock(recordPointer.getBlockID());
            Block block = Block.fromByteArray(byteArray, getRecordSize());
            sb.append(String.format("%d. ", i));
            sb.append(block);
            sb.append("\n");
        }
        return sb.toString();
    }

    public String getNodeLog() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= accessedNodes.size(); i++) {
            Node node = accessedNodes.get(i);
            sb.append(String.format("%d. ", i));
            sb.append(node);
            sb.append("\n");
        }
        return sb.toString();
    }

    public int getNumBlockAccess() {
        return blockAccessCount;
    }

    public int getNumNodeAccess() {
        return nodeAccessCount;
    }

    public int getDeletedNodeCount() {
        return deletedNodeCount;
    }
}
