/**
 * Class representing result of deletion in B+ tree (wrapper class for returning values in deletion)
 */
public class DeleteResult {

    /**
     * Index of deleted child node if any, otherwise null
     */
    private Integer oldChildIndex;

    private Node parentNode;

    /**
     * Indicates if an entry has been deleted
     */
    private boolean found;

    /**
     * Construct a DeleteResult object
     * @param oldChildIndex index of deleted child node if any, otherwise null
     * @param parentNode parentNode of the current node
     * @param found indicates if an entry has been deleted
     */
    public DeleteResult(Integer oldChildIndex, Node parentNode, boolean found) {
        this.oldChildIndex = oldChildIndex;
        this.parentNode = parentNode;
        this.found = found;
    }

    public Integer getOldChildIndex() {
        return oldChildIndex;
    }

    public void setOldChildIndex(Integer oldChildIndex) {
        this.oldChildIndex = oldChildIndex;
    }

    public Node getParentNode() {
        return parentNode;
    }

    public void setParentNode(Node parentNode) {
        this.parentNode = parentNode;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }
}