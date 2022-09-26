import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Storage {
    private final int BLOCK_SIZE;
    private final int RECORD_SIZE;
    private final int NUM_OF_RECORD;
    private final int MEMORY_SIZE;

    private byte[] blocks ;
    private int blockTailIdx;
    private LinkedList<RecordPointer> emptyRecord;

    private BPlusTree bpt;
    private AccessLogger accLog;


    public Storage(int blockSize, int recordSize, int memorySize) {
        BLOCK_SIZE = blockSize;
        RECORD_SIZE = recordSize;
        NUM_OF_RECORD = BLOCK_SIZE / RECORD_SIZE;
        MEMORY_SIZE = memorySize;

        blocks = new byte[MEMORY_SIZE];
        blockTailIdx = -1;
        emptyRecord = new LinkedList<>();
        accLog = new AccessLogger(this);
    }

    public void initWithTSV(String path) {
        try {
            Reader in = new FileReader(path);
            BufferedReader buf = new BufferedReader(in);
            String line = buf.readLine();
            line = buf.readLine();
            try {
                while (line != null) {
                    String[] lineItems = line.split("\\s"); //splitting the line and adding its items in String[]
                    createRecord(lineItems[0], Float.parseFloat(lineItems[1]), Integer.parseInt(lineItems[2]));
                    line = buf.readLine();
                } buf.close();
            } catch(Exception e) {
            	System.out.println("Something went wrong"+e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Wrong file path");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error while reading file");
            e.printStackTrace();
        }
    }

    /**
     * Build B+ tree on database by inserting the records from database sequentially
     */
    public void buildIndex() {
        bpt = new BPlusTree(Util.getNFromBlockSize(BLOCK_SIZE), this);
        for (int blockID = 0; blockID <= blockTailIdx; ++blockID) {
            Block block = Block.fromByteArray(readBlock(blockID), RECORD_SIZE);
            for (int recordID = 0; recordID < NUM_OF_RECORD; ++recordID) {
                Record record = block.readRecord(recordID);
                if (!record.isEmpty()) {
                    bpt.insert(record, new RecordPointer(blockID, recordID));
                }
            }
        }
    }

    /**
     * Insert a new record into "disk storage"
     * @param tConst data for the record
     * @param rating data for the record
     * @param numVotes data for the record
     */
    public RecordPointer createRecord(String tConst, float rating, int numVotes) {
        if(emptyRecord.isEmpty()) createBlock();

        RecordPointer address = emptyRecord.element();
        emptyRecord.remove();

        Block block = Block.fromByteArray(readBlock(address.getBlockID()), RECORD_SIZE);
        block.updateRecord(address.getRecordID(), tConst, rating, numVotes);
        updateBlock(address.getBlockID(), block.toByteArray());

        return address;
    }

    /**
     * Read a record given its address
     * @param address address of record to get
     */
    public Record readRecord(RecordPointer address) {
        accLog.addBlock(address);
        return Block.fromByteArray(readBlock(address.getBlockID()), RECORD_SIZE).readRecord(address.getRecordID());
    }

    /**
     * Delete a record given its address, reallocate it for reuse
     * @param address address of record to be deleted
     */
    public void deleteRecord(RecordPointer address) {
        Block block = Block.fromByteArray(readBlock(address.getBlockID()), RECORD_SIZE);
        block.deleteRecord(address.getRecordID());
        updateBlock(address.getBlockID(), block.toByteArray());

        emptyRecord.add(address);
    }

    /**
     * Prepare a new block so that it can be used.
     */
    public void createBlock() {
        blockTailIdx++;
        Block block = Block.empty(BLOCK_SIZE, RECORD_SIZE);
        updateBlock(blockTailIdx, block.toByteArray());
        for(int recordID = 0; recordID < NUM_OF_RECORD ; ++recordID) {
            emptyRecord.add(new RecordPointer(blockTailIdx, recordID));
        }
    }

    /**
     * Update block in "disk storage".
     * @param blockID ID of block to be updated
     * @param data data of the updated block
     */
    public void updateBlock(int blockID, byte[] data) {
        System.arraycopy(data, 0, blocks, blockID * BLOCK_SIZE, BLOCK_SIZE);
    }

    /**
     * Get a block from "disk storage"
     * @param blockID ID of the block to get
     * @return a block with given ID
     */
    public byte[] readBlock(int blockID) {
        return Arrays.copyOfRange(blocks, blockID * BLOCK_SIZE, blockID * BLOCK_SIZE + BLOCK_SIZE);
    }

    public BPlusTree getBPT() {
        return bpt;
    }

    /**
     * Search for records given the key (numVotes), using index
     * @param searchKey search key value
     * @return list of records matching the key value
     */
    public List<Record> searchBPT(int searchKey) {
        List<RecordPointer> recordAddresses=  bpt.search(searchKey);
        List<Record> records = new LinkedList<>();
        for(RecordPointer ra : recordAddresses) {
            records.add(readRecord(ra));
        }
        return records;
    }

    /**
     * Search for records given the lower and upper bounds, using index
     * @param lower lower bound of search key value, inclusive (numVotes)
     * @param upper upper bound of search key value, inclusive (numVotes)
     * @return list of records having the key value within the lower and upper bounds
     */
    public List<Record> searchBPT(int lower, int upper) {
        List<RecordPointer> recordAddresses=  bpt.search(lower, upper);
        List<Record> records = new LinkedList<>();
        for(RecordPointer ra : recordAddresses) {
            records.add(readRecord(ra));
        }
        return records;
    }

    /**
     * Delete records that match the key value, using index
     * @param deleteKey
     */
    public void deleteBPT(int deleteKey) {
        bpt.delete(deleteKey);
    }

    public int getNumBlocksUsed() {
        return blockTailIdx + 1;
    }

    public int getRecordSize() {
        return RECORD_SIZE;
    }

    public void logNodeAccess(Node node) {
        accLog.addNode(node);
    }

    public void resetLog() {
        accLog.reset();
    }

    public String getNodeLog() {
        return accLog.getNodeAccess();
    }

    public String getBlockLog() {
        return accLog.getBlockAccess();
    }

    public int getNumBlockAccess() {
        return accLog.getNumBlockAccess();
    }

    public int getNumNodeAccess() {
        return accLog.getNumNodeAccess();
    }
}