package rs.myst;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OpCode {
    NOP("nop", 1),                 // 0
    LOAD_0("load_0", 1),           // 1
    LOAD_1("load_1", 1),           // 2
    LOAD_2("load_2", 1),           // 3
    LOAD_3("load_3", 1),           // 4
    LOAD_4("load_4", 1),           // 5
    LOAD_5("load_5", 1),           // 6
    LOAD("load", 5),               // 7
    STORE_0("store_0", 1),         // 8
    STORE_1("store_1", 1),         // 9
    STORE_2("store_2", 1),         // 10
    STORE_3("store_3", 1),         // 11
    STORE_4("store_4", 1),         // 12
    STORE_5("store_5", 1),         // 13
    STORE("store", 5),             // 14
    LOAD_GLOBAL("lglobal", 5),     // 15
    STORE_GLOBAL("sglobal", 5),    // 16
    LOAD_FIELD("lfield", 2),       // 17
    STORE_FIELD("sfield", 2),      // 18
    CONST("const", 5),             // 19
    CONST_M1("const_m1", 1),       // 20
    CONST_0("const_0", 1),         // 21
    CONST_1("const_1", 1),         // 22
    CONST_2("const_2", 1),         // 23
    CONST_3("const_3", 1),         // 24
    CONST_4("const_4", 1),         // 25
    CONST_5("const_5", 1),         // 26
    ADD("add", 1),                 // 27
    SUB("sub", 1),                 // 28
    MUL("mul", 1),                 // 29
    DIV("div", 1),                 // 30
    REM("rem", 1),                 // 31
    NEG("neg", 1),                 // 32
    SHL("shl", 1),                 // 33
    SHR("shr", 1),                 // 34
    INC("inc", 3),                 // 35
    NEW("new", 5),                 // 36
    NEW_ARRAY("newarr", 2),        // 37
    ARRAY_LOAD("aload", 1),        // 38
    ARRAY_STORE("astore", 1),      // 39
    BARRAY_LOAD("baload", 1),      // 40
    BARRAY_STORE("bastore", 1),    // 41
    LENGTH("length", 1),           // 42
    POP("pop", 1),                 // 43
    DUP("dup", 1),                 // 44
    DUP2("dup2", 1),               // 45
    JMP("jmp", 3),                 // 46
    JEQ("jeq", 3),                 // 47
    JNE("jne", 3),                 // 48
    JGT("jgt", 3),                 // 49
    JLE("jle", 3),                 // 50
    JLT("jlt", 3),                 // 51
    JGE("jge", 3),                 // 52
    CALL("call", 3),               // 53
    RETURN("return", 1),           // 54
    ENTER("enter", 3),             // 55
    EXIT("exit", 1),               // 56
    READ("read", 1),               // 57
    PRINT("print", 1),             // 58
    BREAD("bread", 1),             // 59
    BPRINT("bprint", 1),           // 60
    TRAP("trap", 2);               // 61

    private final String niceName;
    private final int size;
}
