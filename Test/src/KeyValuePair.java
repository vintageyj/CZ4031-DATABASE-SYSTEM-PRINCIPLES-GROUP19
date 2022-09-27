/**
 * DEPRECATED Class representing key-value pair in the leaf node of a B+ tree
 */
public class KeyValuePair implements Comparable<KeyValuePair> {

    /**
     * Key
     */
    private Key key;

    /**
     * Value (record address)
     */
    private RecordPointer recordAddress;

    /**
     * Construct a key-value pair
     * @param key key
     * @param recordAddress value (record address)
     */
    public KeyValuePair(Key key, RecordPointer recordAddress) {
        this.key = key;
        this.recordAddress = recordAddress;
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public RecordPointer getRecordAddress() {
        return recordAddress;
    }

    public void setRecordAddress(RecordPointer recordAddress) {
        this.recordAddress = recordAddress;
    }

    public int compareTo(KeyValuePair k) {
        if (k == null) return -1;
        return this.key.compareTo(k.key);
    }
}