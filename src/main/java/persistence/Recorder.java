package persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import simulation.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Recorder {
    private final List<Snapshot> trace = new ArrayList<>();

    private final List<Node> nodes;
    private int stepID = 0;

    public Recorder(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void record(int vCurId) {
        trace.add(new Snapshot(stepID, vCurId, nodes.stream().map(NodeState::new).toList()));
        stepID++;
    }

    //in case we want to simulate new entries?
    public void addNode(Node node) {
        nodes.add(node);
    }

    public void saveToFile(Path file) {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            mapper.writeValue(file.toFile(), trace);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save trace to file: " + file, e);
        }
    }
}
