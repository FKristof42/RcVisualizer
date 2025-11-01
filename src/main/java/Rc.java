public class Rc {

    private int c;
    private Node vCur;
    private int pin;
    private int color;
    //TODO map with colors

    public Rc(int c) {
        if (c < 2)
            throw new IllegalArgumentException("C must be greater or equal to 2!");
        this.c = c;
        color = randomColor(c);
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
        int iter = 0;
        while (/*true*/ iter < 10) {
            //TODO exit condition - in theory it never ends, but we should check whether all nodes were reached.
            color = randomColorExcluding(c, color);

            System.out.println("Starting iteration " + iter + " with color " + color + " form node:");
            System.out.print("\t");
            printVCur();

            vCur.color = color; //TODO local variable? not necessary prolly
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
                        migrate(vCur, nextR()); //Type II backtracking
                        System.out.println("Type II backtracking from node " + vCur.id);
                    }
                    else {
                        goForward(nextR());
                    }
                }
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
        Node vlast = vCur;
        if (i >= v.neighbors.size())
            throw new IllegalArgumentException("Index out of bounds for node: " + vCur.id + ", index: " + i);
        vCur = v.neighbors.get(i);
        pin = vCur.neighbors.indexOf(vlast);
//        System.out.println("->Node " + vCur.id + ", " + vCur.color + " (pin: " + pin + ", parent: " + vCur.parent  +")");
        System.out.print("->");
        printVCur();
    }

    private int nextR() {
        return (pin + 1) % vCur.delta();
    }

    public static void main(String[] args) {
        Rc algorithm = new Rc(5);

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

        algorithm.traverse(n1);
    }
}
