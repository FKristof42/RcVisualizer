package persistence;

import java.util.List;

public class Snapshot {
    public int stepID;
    public int vCurId;
    public List<NodeState> nodeStates;

    public Snapshot(int stepID, int vCurId, List<NodeState> nodeStates) {
        this.stepID = stepID;
        this.vCurId = vCurId;
        this.nodeStates = nodeStates;
    }
}


