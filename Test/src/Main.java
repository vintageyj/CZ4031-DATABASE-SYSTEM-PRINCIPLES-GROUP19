import java.util.List;

public class Main implements Constants {

    public static void main(String[] args) {
        System.out.println("Running Application");

        for (int blockSize : new int[] { BLOCK_SIZE_1, BLOCK_SIZE_2 }) {
            System.out.println();
            System.out.println("===============================================");
            System.out.printf("BLOCK SIZE: %d bytes\n", blockSize);

            Storage st = new Storage(blockSize, RECORD_SIZE, MEMORY_SIZE);
            st.initWithTSV("C:/Users/tomth/demos/CZ4031-DATABASE-SYSTEM-PRINCIPLES-GROUP19/Test/data.tsv");

            // Experiment 1
            System.out.println("EXPERIMENT 1");
            System.out.println("Number of blocks: " + st.getNumBlocksUsed());
            System.out.println(String.format("Size of database: %dMB ( %d bytes )",
                    st.getNumBlocksUsed() * blockSize / MB, st.getNumBlocksUsed() * blockSize));
            // System.out.println("Size of database: " + st.getNumBlocksUsed() * blockSize /
            // MB + " MB (" + st.getNumBlocksUsed() * blockSize + " bytes)");

            // Experiment 2
            Node.setStorage(st);
            Node.setNFromBlockSize(blockSize);
            RecordNode.setMaxSizeFromBlockSize(blockSize);
            st.buildIndex();
            System.out.println("\n\nEXPERIMENT 2");
            System.out.println("Parameter n of B+ tree: " + Node.getN());
            System.out.println(
                    "Number of nodes in B+ tree (Including linked list nodes): " + Node.getTotalNodes(st.getBPT()));
            System.out.println("Height of B+ tree: " + st.getBPT().getHeight());
            System.out.println("Content of root node: " + st.getBPT());
            System.out.println(
                    "Content of first child of root node: " + ((InternalNode) st.getBPT()).getPointers()[0]);

            // Experiment 3
            List<Record> recordsExpt3 = st.searchBPT(EXPERIMENT_3_KEY);
            System.out.println("\n\nEXPERIMENT 3");
            System.out.println("Number of index nodes accessed: " + st.getNumNodeAccess());
            System.out
                    .println("Index nodes accessed (linked list nodes do not count since they only contain pointers):");
            System.out.print(st.getNodeLog());
            System.out.println("Number of blocks accessed: " + st.getNumBlockAccess());
            System.out.println("Blocks accessed:");
            System.out.print(st.getBlockLog());
            double avgOfAvgRatingExpt3 = 0.0;
            for (Record r : recordsExpt3) {
                avgOfAvgRatingExpt3 += r.getaverageRating();
            }
            avgOfAvgRatingExpt3 /= recordsExpt3.size();
            System.out.println("Average of averageRatings returned: " + avgOfAvgRatingExpt3);

            // Experiment 4
            List<Record> recordsExpt4 = st.searchBPT(EXPERIMENT_4_LOWER, EXPERIMENT_4_UPPER);
            System.out.println("\n\nEXPERIMENT 4");
            System.out.println("Number of index nodes accessed: " + st.getNumNodeAccess());
            System.out.println("Index nodes accesses:");
            System.out.println(st.getNodeLog());
            System.out.println("Number of block accessed: " + st.getNumBlockAccess());
            System.out.println("Block accessed:");
            System.out.print(st.getBlockLog());
            double avgOfAvgRatingExpt4 = 0.0;
            for (Record r : recordsExpt4) {
                avgOfAvgRatingExpt4 += r.getaverageRating();
            }
            avgOfAvgRatingExpt4 /= recordsExpt4.size();
            System.out.println("Average of averageRatings returned: " + avgOfAvgRatingExpt4);

            // Experiment 5
            st.deleteBPT(st.getBPT(), EXPERIMENT_5_KEY);
            System.out.println("\n\nEXPERIMENT 5");
            System.out.println("Total number of deleted nodes: " + st.getDeletedNodeCount());
            System.out.println("Number of nodes of updated B+ tree: " + Node.getTotalNodes(st.getBPT()));
            System.out.println("Height of updated B+ tree: " + st.getBPT().getHeight());
            System.out.println("Content of root node: " + st.getBPT());
            System.out.println(
                    "Content of first child of root node: " + ((InternalNode) st.getBPT()).getPointers()[0]);
        }
    }
}