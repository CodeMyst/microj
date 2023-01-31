package rs.myst;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static rs.myst.OpCode.*;

@Getter
@RequiredArgsConstructor
public enum Condition {
    EQUALS(JEQ, JNE),
    NOT_EQUALS(JNE, JEQ),
    LESS(JLT, JGE),
    LESS_EQUAL(JLE, JGT),
    GREATER_EQUAL(JGE, JLT),
    GREATER(JGT, JLE);

    private final OpCode jumpOpCode;
    private final OpCode reverseJumpOpCode;

    public static Condition fromToken(TokenKind kind) {
        return switch (kind) {
            case EQUALS -> EQUALS;
            case NOT_EQUALS -> NOT_EQUALS;
            case LESS -> LESS;
            case LESS_EQUAL -> LESS_EQUAL;
            case GREATER_EQUAL -> GREATER_EQUAL;
            case GREATER -> GREATER;
            default -> null;
        };
    }
}
