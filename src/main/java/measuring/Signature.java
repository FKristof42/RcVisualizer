package measuring;

import java.util.List;

public class Signature {
    public int maxColors;
    public int maxNodes;
    public int nodeStep;
    public int runsCount;
    public List<Measurement> measurements;

    public Signature(int maxColors, int maxNodes, int nodeStep, int runsCount, List<Measurement> measurements) {
        this.maxColors = maxColors;
        this.maxNodes = maxNodes;
        this.nodeStep = nodeStep;
        this.runsCount = runsCount;
        this.measurements = measurements;
    }
}
