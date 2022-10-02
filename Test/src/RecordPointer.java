/**
 * Class representing the logical address of a record
 */
public class RecordPointer {

    /**
     * Block ID
     */
    private int blockID;

    /**
     * Record ID
     */
    private int recordID;

    /**
     * Construct record address
     * @param blockID  block id
     * @param recordID record id
     */
    public RecordPointer(int blockID, int recordID) {
        this.blockID = blockID;
        this.recordID = recordID;
    }

    public int getBlockID() {
        return blockID;
    }

    public int getRecordID() {
        return recordID;
    }
}