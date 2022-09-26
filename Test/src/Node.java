/**
 * Interface representing a node in a B+ tree
 */
public abstract class Node {

    abstract int getDegree();

    abstract void setDegree(int degree);

    abstract InternalNode getParent();

    abstract void setParent(InternalNode parent);

    public abstract String toString();
}