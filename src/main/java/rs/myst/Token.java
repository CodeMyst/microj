package rs.myst;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Token {
    private TokenKind kind;

    private int line;
    private int col;

    /**
     * Int or char value.
     */
    private int value;

    /**
     * String value.
     */
    private String string;

    public Token(int line, int col) {
        this.line = line;
        this.col = col;
    }

    public String toString() {
        final String base = String.format("line: %d, col: %d, kind: %s", line, col, kind);

        if (kind == TokenKind.IDENTIFIER) {
            return base.concat(", value: ").concat(string);
        } else if (kind == TokenKind.NUMBER) {
            return base.concat(", value: ").concat(String.valueOf(value));
        } else if (kind == TokenKind.CHAR) {
            return base.concat(", value: ").concat(String.valueOf((char) value));
        }

        return base;
    }
}
