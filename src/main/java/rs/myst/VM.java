package rs.myst;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class VM {
    private static int pc;
    private static int fbp, fsp, esp;
    private static int freep = 1; // 0 is reserved for null

    private static final int HEAP_SIZE_WORDS = 100_000;
    private static final int FSTACK_SIZE_WORDS = 400;
    private static final int ESTACK_SIZE_WORDS = 30;

    private static final int WORD_BYTES = 4;
    private static final int SHORT_BYTES = 2;

    private static byte[] codeData;
    private static int[]  globalData = new int[100]; // TODO
    private static int[]  heap   = new int[HEAP_SIZE_WORDS];
    private static final int[]  estack = new int[ESTACK_SIZE_WORDS];
    private static final int[]  fstack = new int[FSTACK_SIZE_WORDS];

    private static Scanner input;

    public static void runFromFile(String filePath) throws IOException {
        File inputFile = new File(filePath);
        InputStream in = new FileInputStream(inputFile);
        long inputFileSize = inputFile.length();

        if (inputFileSize > 3000) {
            throw new IllegalArgumentException("File too large");
        }

        codeData = new byte[(int) inputFileSize];
        in.read(codeData);
        in.close();

        char M = (char) getByte(0);
        char J = (char) getByte(1);

        if (M != 'M' || J != 'J') {
            throw new IllegalArgumentException("Illegal file format");
        }

        pc = getWord(2);
        getWord(4);

        input = new Scanner(System.in);

        execute();
    }

    private static void execute() {
        var instructions = OpCode.values();

        while (true) {
            OpCode instruction = instructions[getByte()];

            switch (instruction) {
                /* Loading and storing */

                case CONST:
                    epush(getWord());
                    break;

                case CONST_0: case CONST_1: case CONST_2: case CONST_3: case CONST_4: case CONST_5:
                    epush(instruction.ordinal() - OpCode.CONST_0.ordinal());
                    break;


                case LOAD:
                    epush(fstack[fbp + getByte()]);
                    break;

                case LOAD_0: case LOAD_1: case LOAD_2: case LOAD_3: case LOAD_4: case LOAD_5:
                    int localLoadAddress = instruction.ordinal() - OpCode.LOAD_0.ordinal();
                    epush(fstack[fbp + localLoadAddress]);
                    break;


                case STORE:
                    fstack[fbp + getShort()] = epop();
                    break;

                case STORE_0: case STORE_1: case STORE_2: case STORE_3: case STORE_4: case STORE_5:
                    int localStoreAddress = instruction.ordinal() - OpCode.STORE_0.ordinal();
                    fstack[fbp + localStoreAddress] = epop();
                    break;

                case LOAD_GLOBAL:
                    int address = getShort();
                    epush(globalData[address]);
                    break;

                case STORE_GLOBAL:
                    address = getShort();
                    globalData[address] = epop();
                    break;


                /* Arrays */

                case NEW_ARRAY:
                    int length = epop();
                    int elementSize = getByte();

                    if (length < 0) {
                        System.err.println("Cannot initialize an array with less than 0 elements.");
                    }

                    heap[freep] = length;
                    heap[freep + 1] = elementSize;

                    epush(malloc(elementSize, length) + 2);
                    break;

                case ARRAY_LOAD:
                    int index = epop();
                    address = epop();

                    if (address == 0) {
                        System.err.println("Null pointer error");
                    }

                    length = heap[address - 2];

                    if (index < 0 || index >= length) {
                        System.err.printf("Index %d for length %d%n", index, length);
                    }

                    elementSize = heap[address - 1];

                    epush(heap[address + index * elementSize]);
                    break;

                case ARRAY_STORE:
                    int value = epop();
                    index = epop();
                    address = epop();

                    if (address == 0) {
                        System.err.println("Null pointer error");
                    }

                    length = heap[address - 2];
                    elementSize = heap[address - 1];

                    if (index < 0 || index >= length) {
                        System.err.printf("Index %d for length %d%n", index, length);
                    }

                    heap[address + index * elementSize] = value;
                    break;

                case BARRAY_LOAD:
                    index = epop();
                    address = epop();

                    if (address == 0) {
                        System.err.println("Null pointer error");
                    }

                    length = heap[address - 2];

                    if (index < 0 || index >= length) {
                        System.err.printf("Index %d for length %d%n", index, length);
                    }

                    int word = heap[address + index / 4];
                    int shiftAmount = 8 * (3 - index % 4);
                    word >>= shiftAmount;

                    epush((byte) word);
                    break;

                case BARRAY_STORE:
                    byte b = (byte) epop();
                    index = epop();
                    address = epop();

                    if (address == 0) {
                        System.err.println("Null pointer error");
                    }

                    length = heap[address - 2];

                    if (index < 0 || index >= length) {
                        System.err.printf("Index %d for length %d%n", index, length);
                    }

                    word = heap[address + index / 4];
                    shiftAmount = 8 * (3 - index % 4);

                    int insertValue = b << shiftAmount;
                    int clearByteMask = ~(0xff << shiftAmount);

                    heap[address + index / 4] = word & clearByteMask | insertValue;
                    break;

                case LENGTH:
                    address = epop() - 2;
                    if (address < 0) {
                        System.err.println("Null pointer error");
                    }
                    epush(heap[address]);
                    break;

                /* Structs */

                case NEW:
                    int fieldCount = getByte() & 0xff;
                    epush(malloc(WORD_BYTES, fieldCount));
                    break;

                case STORE_FIELD:
                    value = epop();
                    int structAddress = epop();

                    if (structAddress == 0) {
                        System.err.println("ERROR");
                    }

                    int fieldIndex = getByte() & 0xff;
                    heap[structAddress + fieldIndex] = value;
                    break;

                case LOAD_FIELD:
                    structAddress = epop();
                    if (structAddress == 0) {
                        System.err.println("ERROR");
                    }

                    fieldIndex = getByte();
                    epush(heap[structAddress + fieldIndex]);
                    break;

                /* Operations */

                case ADD:
                    epush(epop() + epop());
                    break;

                case SUB:
                    var v1 = epop();
                    var v2 = epop();
                    epush(v2 - v1);
                    break;

                case DIV:
                    epush(epop() / epop());
                    break;

                case MUL:
                    epush(epop() * epop());
                    break;

                case REM:
                    epush(epop() % epop());
                    break;

                /* Jumps */

                case JMP:
                    int jumpAmount = getShort();
                    pc += jumpAmount - 3;
                    break;

                case JEQ:
                    jumpAmount = getShort();
                    if (epop() == epop()) pc += jumpAmount - 3;
                    break;

                case JNE:
                    jumpAmount = getShort();
                    if (epop() != epop()) pc += jumpAmount - 3;
                    break;

                case JGE:
                    jumpAmount = getShort();
                    if (epop() <= epop()) pc += jumpAmount - 3;
                    break;

                case JGT:
                    jumpAmount = getShort();
                    if (epop() < epop()) pc += jumpAmount - 3;
                    break;

                case JLE:
                    jumpAmount = getShort();
                    if (epop() >= epop()) pc += jumpAmount - 3;
                    break;

                case JLT:
                    jumpAmount = getShort();
                    if (epop() > epop()) pc += jumpAmount - 3;
                    break;


                /* IO */

                case BPRINT:
                    System.out.print((char) epop());
                    break;

                case PRINT:
                    System.out.print(epop());
                    break;

                case READ:
                    epush(input.nextInt());
                    break;

                case BREAD:
                    epush(input.nextByte());
                    break;

                case CALL:
                    int callAddress = getShort();
                    fpush(pc);
                    pc = callAddress;
                    break;

                case RETURN:
                    if (fsp == 0) return; // no caller = main, exit
                    pc = fpop(); // get pc that was saved before calling
                    break;

                case ENTER:
                    int paramsCount = getByte();
                    int localsCount = getByte();

                    fpush(fbp);  // save base pointer
                    fbp = fsp;   // base pointer is at top of old stack frame

                    // Space for locals and parameters
                    for (int i = 0; i < paramsCount; i++) fpush(0);
                    for (int i = 0; i < localsCount; i++) fpush(0);

                    // Loading parameters from estack in reverse
                    for (int i = paramsCount - 1; i >= 0; i--) fstack[fbp + i] = epop();

                    break;

                case EXIT:
                    fsp = fbp;      // base is old stack top
                    fbp = fpop();   // retrieve previously saved base pointer
                    break;

                case NOP:
                    break;

                default:
                    System.err.println("Instruction not implemented: " + instruction.getNiceName());
            }
        }
    }

    private static void error(String msg) {
        System.err.println("Error: " + msg);
        System.exit(1);
    }

    private static void epush(int x) {
        if (esp == ESTACK_SIZE_WORDS) error("Expression stack overflow");
        estack[esp++] = x;
    }

    private static int epop() {
        if (esp == 0) error("Tried to pop empty expression stack");
        return estack[--esp];
    }

    private static void fpush(int x) {
        if (fsp == FSTACK_SIZE_WORDS) error("Frame stack overflow");
        fstack[fsp++] = x;
    }

    private static int fpop() {
        if (fsp == 0) error("Tried to pop empty frame stack");
        return fstack[--fsp];
    }


    private static byte getByte() {
        return codeData[pc++];
    }

    private static byte getByte(int address) {
        return codeData[address];
    }

    private static short getShort() {
        return (short) (((short) getByte() << 8) | (getByte() & 0b11111111));
    }

    private static short getShort(int address) {
        return (short) (((short) getByte(address) << 8) | (getByte(address + 1) & 0b11111111));
    }

    private static int getWord() {
        return (getShort() << 16) | (getShort() & 0b11111111_11111111);
    }

    private static int getWord(int address) {
        return (getShort(address) << 16) | (getShort(address + 2) & 0b11111111_11111111);
    }

    /**
     * Allocate n blocks of memory, size bytes each
     */
    private static int malloc(int size, int n) {
        int address = freep;
        int bytes = size * n;
        int words = (bytes + 3) / 4;

        freep += words;

        return address;
    }
}
