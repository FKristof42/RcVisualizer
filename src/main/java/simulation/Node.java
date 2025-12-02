package simulation;

import java.util.*;

public class Node {

    public int id; //for visualization purposes - no unique ID assumed
    public int color;
    //-1 represents \bottom
    public int parent;
    public List<Node> neighbors = new ArrayList<>();

    public Node(int c, int id) {
        if (c < 2)
            throw new IllegalArgumentException("C must be greater or equal to 2!");
        this.color = simulation.Rc.randomColor(c);

        this.id = id;
    }

    public int delta() {
        return neighbors.size();
    }
    public static void createEdge(Node a, Node b) {
        a.neighbors.add(b);
        b.neighbors.add(a);

    }

    /**
     * Generates a random connected undirected graph with n nodes and approximately the given density.
     * When the density is too low to ensure connectivity, it is adjusted upwards (to n - 1 edges).
     */
    public static List<Node> generateRandomConnectedGraph(int n, double density, int c) {
        if (n < 1)
            throw new IllegalArgumentException("n must be >= 1");
        if (density < 0.0 || density > 1.0)
            throw new IllegalArgumentException("density must be in [0,1]");

        Random rnd = new Random();
        List<Node> nodes = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            nodes.add(new Node(c, i));
        }

        long possibleEdges = (long) n * (n - 1) / 2;
        long desiredEdges = Math.round(density * possibleEdges);
        if (desiredEdges < n - 1) {
            desiredEdges = n - 1; // ensure connectivity
        }

        // Create a random spanning tree first (n-1 edges)
        List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) order.add(i);
        Collections.shuffle(order, rnd);

        // track existing edges as packed long (min<<32 | max)
        Set<Long> existing = new HashSet<>();
        for (int i = 1; i < n; i++) {
            int uIdx = order.get(i);
            int vIdx = order.get(rnd.nextInt(i)); // connect to a random earlier node
            Node.createEdge(nodes.get(uIdx), nodes.get(vIdx));
            existing.add(packEdge(Math.min(uIdx, vIdx), Math.max(uIdx, vIdx)));
        }

        // Add random extra edges until desiredEdges reached
        long currentEdges = existing.size();
        if (currentEdges < desiredEdges) {
            int attempts = 0;
            int maxAttempts = 10_000_000; // safety cap to avoid infinite loops
            while (currentEdges < desiredEdges && attempts < maxAttempts) {
                attempts++;
                int a = rnd.nextInt(n);
                int b = rnd.nextInt(n);

                if (a == b)
                    continue;

                int x = Math.min(a, b), y = Math.max(a, b);
                long key = packEdge(x, y);
                if (existing.contains(key))
                    continue;

                Node.createEdge(nodes.get(x), nodes.get(y));
                existing.add(key);
                currentEdges++;
                attempts = 0;
            }

            if (currentEdges < desiredEdges) {
                for (int i = 0; i < n && currentEdges < desiredEdges; i++) {
                    for (int j = i + 1; j < n && currentEdges < desiredEdges; j++) {
                        long key = packEdge(i, j);
                        if (!existing.contains(key)) {
                            Node.createEdge(nodes.get(i), nodes.get(j));
                            existing.add(key);
                            currentEdges++;
                        }
                    }
                }
            }
        }

        return nodes;
    }

    private static long packEdge(int a, int b) {
        return (((long) a) << 32) | (b & 0xffffffffL);
    }
}
