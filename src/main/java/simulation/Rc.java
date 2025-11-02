package simulation;

import persistence.Recorder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class Rc {

    private final int c;

    private final Recorder recorder;

    private Node vCur;
    private int pin;
    private int color;
    //TODO map with colors - or only in visualization?

    public Rc(int c, Recorder recorder) {
        if (c < 2)
            throw new IllegalArgumentException("C must be greater or equal to 2!");
        this.c = c;
        color = randomColor(c);
        this.recorder = recorder;
    }

    public static int randomColor(int c) {
        return (int) (Math.random() * c);
    }

    public static int randomColorExcluding(int c, int excluding) {
        int color; //TODO
        do {
            color = randomColor(c);
        } while (color == excluding);
        return color;
    }

    private void printVCur() {
        if (vCur.parent > -1)
            System.out.println("Node " + vCur.id + ", " + vCur.color + " (pin: " + pin + ", parent: " + vCur.parent  +" - "+ vCur.neighbors.get(vCur.parent).id +")");
        else System.out.println("Node " + vCur.id + ", " + vCur.color + " (pin: " + pin + ", parent: " + vCur.parent  + ")");
    }

    public void traverse(Node v) {
        vCur = v;
        //TODO shouldn't the first vertex's parent be set to -1?
        int iter = 0;
        while (/*true*/ iter < 10) {
            //TODO exit condition - in theory it never ends, but we should check whether all nodes were reached.
            color = randomColorExcluding(c, color);

            System.out.print("Starting iteration " + iter + " with color " + color + " form node:\n\t");
            printVCur();

            vCur.color = color;
            goForward(0);

            while (!(vCur.parent == -1 && pin == vCur.delta() - 1)) {
                if (vCur.color != color) {
                    vCur.color = color;
                    vCur.parent = pin;
                    goForward(nextR());
                }
                else {
                    if (vCur.parent == nextR()) {
                        vCur.parent = -1;
                        System.out.println("Type II backtracking from node " + vCur.id);
                        migrate(vCur, nextR()); //Type II backtracking
                    }
                    else {
                        goForward(nextR());
                    }
                }

                //TODO maybe more record calls will be necessary? - for example for type I backtracking
                recorder.record(vCur.id);
            }

            iter++;
        }
    }

    private void goForward(int i) {
        migrate(vCur, i);
        if (vCur.color == color) {
            System.out.println("Type I backtracking from node " + vCur.id);
            migrate(vCur, pin); //Type I backtracking
        }
    }

    private void migrate(Node v, int i) {
        Node vLast = vCur;
        if (i >= v.neighbors.size())
            throw new IllegalArgumentException("Index out of bounds for node: " + vCur.id + ", index: " + i);
        vCur = v.neighbors.get(i);
        pin = vCur.neighbors.indexOf(vLast);
//        System.out.println("->simulation.Node " + vCur.id + ", " + vCur.color + " (pin: " + pin + ", parent: " + vCur.parent  +")");
        System.out.print("->");
        printVCur();
        System.out.println("nextR: " + nextR());
    }

    private int nextR() {
        return (pin + 1) % vCur.delta();
    }

    public static void main(String[] args) {
        //TODO random graph generator for testing
        Node n1 = new Node(5, 1);
        Node n2 = new Node(5, 2);
        Node n3 = new Node(5, 3);
        Node n4 = new Node(5, 4);
        Node n5 = new Node(5, 5);

        Node.createEdge(n1, n2);
        Node.createEdge(n2, n3);
        Node.createEdge(n2, n4);
        Node.createEdge(n2, n5);
        Node.createEdge(n3, n5);

        Recorder recorder = new Recorder(Arrays.asList(n1, n2, n3, n4, n5));
        Rc algorithm = new Rc(5, recorder);

        algorithm.traverse(n1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        Path out = Paths.get("target", "traces", "trace-" + timestamp + ".json");
        recorder.saveToFile(out);
        System.out.println("Trace saved to " + out);
    }
}
