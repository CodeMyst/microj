package rs.myst;

public class Label {
    private int address;
    private boolean defined = false;

    public void put() {
        int pc = OpCodeBuffer.pc;

        if (defined) {
            OpCodeBuffer.put2(address - (pc - 1));
        } else {
            OpCodeBuffer.put2(address);
            address = pc;
        }
    }

    public void here() {
        if (defined) {
            System.err.println("This shouldn't happen...");
            System.exit(1);
        }

        while (address != 0) {
            int lastUnresolved = address;
            address = OpCodeBuffer.get2(lastUnresolved);

            OpCodeBuffer.put2(OpCodeBuffer.pc - (lastUnresolved - 1), lastUnresolved);
        }

        defined = true;
        address = OpCodeBuffer.pc;
    }
}
