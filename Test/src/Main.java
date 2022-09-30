import java.util.List;

public class Main implements Constants {

    public static void main(String[] args) {
        System.out.println("Running Application");

        for (int blockSize : new int[] { BLOCK_SIZE_1, BLOCK_SIZE_2 }) {
            System.out.println();
            System.out.println("===============================================");
            System.out.printf("BLOCK SIZE: %d bytes\n", blockSize);

            Storage st = new Storage(blockSize, RECORD_SIZE, MEMORY_SIZE);
            st.initWithTSV("data.tsv");

            // Experiment 1
            System.out.println("EXPERIMENT 1");
            System.out.println("Number of blocks: " + st.getNumBlocksUsed());
            System.out.println(String.format("Size of database: %dMB ( %d bytes )",
                    st.getNumBlocksUsed() * blockSize / MB, st.getNumBlocksUsed() * blockSize));
            // System.out.println("Size of database: " + st.getNumBlocksUsed() * blockSize /
            // MB + " MB (" + st.getNumBlocksUsed() * blockSize + " bytes)");

            // Experiment 2
            st.buildIndex();
            System.out.println("\n\nEXPERIMENT 2");
            System.out.println("Parameter n of B+ tree: " + st.getBPT().getN());
            System.out.println("Number of nodes in B+ tree: " + st.getBPT().getTotalNodes());
            System.out.println("Height of B+ tree: " + st.getBPT().getHeight());
            System.out.println("Content of root node: " + st.getBPT().getRoot());
            System.out.println(
                    "Content of first child of root node: " + ((InternalNode) st.getBPT().getRoot()).getPointers()[0]);

            // Experiment 3
            List<Record> recordsExpt3 = st.searchBPT(EXPERIMENT_3_KEY);
            System.out.println("\n\nEXPERIMENT 3");
            System.out.println("Number of index nodes accessed: " + st.getNumNodeAccess());
            System.out.println("Index nodes accesses:");
            System.out.print(st.getNodeLog());
            System.out.println("Number of block accessed: " + st.getNumBlockAccess());
            System.out.println("Block accessed:");
            System.out.print(st.getBlockLog());
            double avgOfAvgRatingExpt3 = 0.0;
            for (Record r : recordsExpt3) {
                avgOfAvgRatingExpt3 += r.getAvgRating();
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
                avgOfAvgRatingExpt4 += r.getAvgRating();
            }
            avgOfAvgRatingExpt4 /= recordsExpt4.size();
            System.out.println("Average of averageRatings returned: " + avgOfAvgRatingExpt4);

            // Experiment 5
            st.deleteBPT(EXPERIMENT_5_KEY);
            System.out.println("\n\nEXPERIMENT 5");
            System.out.println("Total number of deleted nodes: " + st.getDeletedNodeCount());
            System.out.println("Number of nodes of updated B+ tree: " + st.getBPT().getTotalNodes());
            System.out.println("Height of updated B+ tree: " + st.getBPT().getHeight());
            System.out.println("Content of root node: " + st.getBPT().getRoot());
            System.out.println(
                    "Content of first child of root node: " + ((InternalNode) st.getBPT().getRoot()).getPointers()[0]);
        }
    }
}