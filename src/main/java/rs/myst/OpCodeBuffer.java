package rs.myst;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static rs.myst.OpCode.*;

public class OpCodeBuffer {
    private static final byte[] buffer = new byte[3000];

    private static final int CODE_START_ADDRESS = 10;
    private static final int HEADER_MAIN_ADDRESS = 2;
    private static final int HEADER_SIZE_ADDRESS = 6;

    public static int pc = CODE_START_ADDRESS;
    public static int mainStart;

    public static void put(int x) {
        buffer[pc++] = (byte) x;
    }

    public static void put(int x, int address) {
        buffer[address] = (byte) x;
    }

    public static void put(OpCode opCode) {
        put(opCode.ordinal());
    }

    public static void put2(int x) {
        put(x >> 8);
        put(x);
    }

    public static void put2(int x, int address) {
        buffer[address] = (byte) (x >> 8);
        buffer[address + 1] = (byte) x;
    }

    public static void put4(int x) {
        put2(x >> 16);
        put2(x);
    }

    public static void put4(int x, int address) {
        put2(x >> 16, address);
        put2(x, address + 2);
    }

    public static int get2(int address) {
//        return (buffer[address] << 8) + buffer[address + 1];
        return (buffer[address] << 8) & 0xff00 | (buffer[address + 1] & 0xff);
    }

    public static int get4(int address) {
//        return (get2(address) << 16) + get2(address + 2);
        return (get2(address) << 16) & 0xffff0000 | (get2(address + 2) & 0xffff);
    }

    public static void load(Descriptor d) {
        if (d.getKind() == null) return;

        switch (d.getKind()) {
            case CONSTANT -> {
                if (d.getType().getKind() == TypeKind.NONE) loadConst(0);
                else loadConst(d.getValue());
            }

            case STATIC -> {
                put(LOAD_GLOBAL);
                put2(d.getAddress());
            }

            case LOCAL -> {
                if (d.getAddress() >= 0 && d.getAddress() <= 5) put(LOAD_0.ordinal() + d.getAddress());
                else { put(LOAD.ordinal()); put(d.getAddress()); }
            }

            case FIELD -> {
                put(LOAD_FIELD.ordinal());
                put(d.getAddress());
            }

            case ARRAY_ELEMENT -> {
                if (d.getType().getKind() == TypeKind.CHAR) put(BARRAY_LOAD.ordinal());
                else put(ARRAY_LOAD.ordinal());
            }

            case CONDITION -> put(d.getCondition().getJumpOpCode());

            case STACK, METHOD -> {}

            default -> error("Compiler error.");
        }

        d.setKind(DescriptorKind.STACK);
    }

    public static void assign(Descriptor a, Descriptor b) {
        load(b);

        switch (a.getKind()) {
            case LOCAL -> {
                if (a.getAddress() >= 0 && a.getAddress() <= 5) put(STORE_0.ordinal() + a.getAddress());
                else { put(STORE.ordinal()); put(a.getAddress()); }
            }

            case STATIC -> {
                put(STORE_GLOBAL);
                put2(a.getAddress());
            }

            case FIELD -> {
                put(STORE_FIELD.ordinal());
                put2(a.getAddress());
            }

            case ARRAY_ELEMENT -> {
                if (a.getType().getKind() == TypeKind.CHAR) put(BARRAY_STORE.ordinal());
                else put(ARRAY_STORE.ordinal());
            }

            default -> error("Left-hand side of assignment must be a variable, got: " + a.getKind());
        }
    }

    public static void trueJump(Descriptor conditionDescriptor) {
        put(conditionDescriptor.getCondition().getJumpOpCode());
        conditionDescriptor.getTrueLabel().put();
    }

    public static void falseJump(Descriptor conditionDescriptor) {
        put(conditionDescriptor.getCondition().getReverseJumpOpCode());
        conditionDescriptor.getFalseLabel().put();
    }

    public static void jump(Label label) {
        put(JMP);
        label.put();
    }

    public static void writeHeader() {
        put('M', 0);
        put('J', 1);
        put4(mainStart, HEADER_MAIN_ADDRESS);
        put4(pc - 1, HEADER_SIZE_ADDRESS);
    }

    public static void createObjectFile(String inputFile) {
        String outputFile = inputFile.substring(0, inputFile.lastIndexOf("."));

        writeHeader();

        try (OutputStream os = new FileOutputStream(outputFile + ".obj")) {
            os.write(Arrays.copyOf(buffer, pc));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void printCode() {
        System.out.println("main: " + get4(HEADER_MAIN_ADDRESS));
        System.out.println("size: " + get4(HEADER_SIZE_ADDRESS));

        var instructions = OpCode.values();

        int i = CODE_START_ADDRESS;
        while (i < pc) {
            OpCode instruction = instructions[buffer[i]];

            System.out.printf("%03d\t%s\n", i, instruction.getNiceName());

            int iSize = instruction.getSize();
            if (iSize > 1) {
                int paramValue = 0;

                int j = i + 1;
                while (j < i + iSize) {
                    byte b = buffer[j];

                    paramValue <<= 8;
                    int getSign = (8 * (4 - j + i));
                    paramValue = paramValue << getSign >> getSign;
                    paramValue |= (b & 0b11111111);

                    j++;
                }
            }

            i += instruction.getSize();
        }
    }

    private static void loadConst(int n) {
        if (n >= 0 && n <= 5) put(CONST_0.ordinal() + n);
        else if (n == -1) put(CONST_M1.ordinal());
        else { put(CONST.ordinal()); put4(n); }
    }

    private static void error(String msg) {
        System.out.println(msg);
    }
}
