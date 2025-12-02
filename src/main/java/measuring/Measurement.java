package measuring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import simulation.Node;
import simulation.Rc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Measurement {
    int c;
    int nodeCount;
    double density;
    int runsCount;

    double averageMoves;
    double averageColorChanges;
    double averageIterations;

    public Measurement(int c, int nodeCount, double density, int runsCount) {
        this.c = c;
        this.nodeCount = nodeCount;
        this.density = density;
        this.runsCount = runsCount;

        List<Metrics> runs = new ArrayList<>();
        Rc rc = new Rc(c, null);

        for (int i = 0; i < runsCount; i++) {
            List<Node> nodes = Node.generateRandomConnectedGraph(nodeCount, density, c);

            Metrics m = rc.traverse(nodes);
            runs.add(m);
        }

        int totalMoves = 0;
        int totalColorChanges = 0;
        int totalIterations = 0;

        for (Metrics m : runs) {
            totalMoves += m.moves;
            totalColorChanges += m.colorChanges;
            totalIterations += m.iterations;
        }

        averageMoves = (double) totalMoves / runsCount;
        averageColorChanges = (double) totalColorChanges / runsCount;
        averageIterations = (double) totalIterations / runsCount;
    }

    public synchronized void saveToFile() {
        try {
            Path out = Paths.get(
                    "measurements",
                    "measurement-" + c + "-" + nodeCount + "-" + density + "-" + runsCount + ".json"
            );

            Gson g = new GsonBuilder().setPrettyPrinting().create();
            String json = g.toJson(this);
            Files.createDirectories(out.getParent());
            Files.write(out, json.getBytes());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void main(String[] args) {
        int MAX_COLORS = 10;
        int MAX_NODES = 100;
        int RUNSCOUNT = 100;

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threads));

        List<Future<Measurement>> futures = new ArrayList<>();

        for (int c = 2; c <= MAX_COLORS; c++) {
            for (int n = 10; n <= MAX_NODES; n += 10) {
                for (double d = 0.1; d <= 1.0; d += 0.1) {
                    final int cc = c;
                    final int nn = n;
                    final double dd = d;
                    futures.add(pool.submit(() -> {
                        if (nn % 100 == 0 && dd == 0.1)
                            System.out.println("Starting measurement c=" + cc + " n=" + nn + " d=" + dd);
                        return new Measurement(cc, nn, dd, RUNSCOUNT);
                    }));
                }
            }
        }

        List<Measurement> measurements = new ArrayList<>();

        // wait for all tasks to finish and surface exceptions
        try {
            for (Future<Measurement> f : futures) {
                measurements.add(f.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } finally {
            pool.shutdown();
        }

        try {
            Path out = Paths.get("measurements", "measurement.json");

            Gson g = new GsonBuilder().setPrettyPrinting().create();
            String json = g.toJson(measurements);
            Files.createDirectories(out.getParent());
            Files.write(out, json.getBytes());
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}
