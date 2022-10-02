/**
 * Class representing a record in storage
 */
public class Record {

    /**
     * Empty flag
     */
    private boolean empty; //1bit

    /**
     * tconst attribute
     */
    private char[] tconst; //2*x

    /**
     * Average rating attribute
     */
    private float averageRating; //4

    /**
     * Number of votes attribute
     */
    private int numVotes; //4

    /**
     * Construct an empty record
     */
    public Record() {
        tconst = new char[10];
        empty = true;
    }

    /**
     * Construct an empty record
     * @return empty record
     */
    /* 
    public static Record empty() {
        return new Record();
    }
    */

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public char[] getTconst() {
        return tconst;
    }

    public void setTconst(char[] tconst) {
        this.tconst = tconst;
    }

    public float getaverageRating() {
        return averageRating;
    }

    public void setaverageRating(float averageRating) {
        this.averageRating = averageRating;
    }

    public int getNumVotes() {
        return numVotes;
    }

    public void setNumVotes(int numVotes) {
        this.numVotes = numVotes;
    }

    @Override
    public String toString() {
        return String.valueOf(tconst).trim();
    }
}