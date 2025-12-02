package measuring;

import java.util.List;

public class Signature {
    public int maxColors;
    public int maxNodes;
    public int runsCount;
    public List<Measurement> measurements;

    public Signature(int maxColors, int maxNodes, int runsCount, List<Measurement> measurements) {
        this.maxColors = maxColors;
        this.maxNodes = maxNodes;
        this.runsCount = runsCount;
        this.measurements = measurements;
    }
}
