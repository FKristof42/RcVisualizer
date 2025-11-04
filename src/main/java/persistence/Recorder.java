package persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Recorder with activeColor field added to Step. */
public class Recorder {
    public static class NodeState { 
        public int id; 
        public int color; 
        public int parent; 
        public List<Integer> neighborIds; 
    }
    
    public static class Step { 
        public int stepID; 
        public int vCurId; 
        public int activeColor;  // NEW: track the active color
        public List<NodeState> nodeStates; 
    }

    private final List<Step> steps = new ArrayList<>();
    private int nextStepId = 0;

    public synchronized void recordMove(Object vCurGeneric, List<?> allNodesGeneric, int activeColor) {
        Step s = buildStep(vCurGeneric, allNodesGeneric, activeColor);
        s.stepID = nextStepId++;
        steps.add(s);
    }

    public synchronized void recordColorChange(Object vCurGeneric, List<?> allNodesGeneric, int activeColor) {
        Step s = buildStep(vCurGeneric, allNodesGeneric, activeColor);
        s.stepID = nextStepId++;
        steps.add(s);
    }

    private Step buildStep(Object vCurGeneric, List<?> allNodesGeneric, int activeColor) {
        Step s = new Step();
        s.activeColor = activeColor;  // NEW: store activeColor
        
        try {
            int vCurId = (int) vCurGeneric.getClass().getField("id").get(vCurGeneric);
            s.vCurId = vCurId;
        } catch (Exception e) { throw new RuntimeException(e); }

        s.nodeStates = new ArrayList<>();
        for (Object o : allNodesGeneric) {
            try {
                int id = (int) o.getClass().getField("id").get(o);
                int color = (int) o.getClass().getField("color").get(o);
                int parent = (int) o.getClass().getField("parent").get(o);
                @SuppressWarnings("unchecked")
                List<?> neigh = (List<?>) o.getClass().getField("neighbors").get(o);

                NodeState ns = new NodeState();
                ns.id = id; ns.color = color; ns.parent = parent;
                ns.neighborIds = new ArrayList<>();
                for (Object nb : neigh) {
                    int nbId = (int) nb.getClass().getField("id").get(nb);
                    ns.neighborIds.add(nbId);
                }
                s.nodeStates.add(ns);
            } catch (Exception ex) { throw new RuntimeException(ex); }
        }
        return s;
    }

    public synchronized void saveToFile(Path out) {
        try {
            Gson g = new GsonBuilder().setPrettyPrinting().create();
            String json = g.toJson(steps);
            Files.createDirectories(out.getParent());
            Files.write(out, json.getBytes());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public synchronized List<Step> getSteps() { return new ArrayList<>(steps); }
}