package rs.myst;

import java.io.IOException;
import java.io.Reader;

public class Scanner {
    private final Reader input;

    private char nextChar;
    private boolean reachedEOF;

    private int line = 1;
    private int col = 0;

    private Token lastToken;

    public Scanner(Reader inputReader) {
        this.input = inputReader;

        getNextChar();
    }

    public boolean hasNext() {
        return lastToken.getKind() != TokenKind.INVALID && lastToken.getKind() != TokenKind.EOF;
    }

    public Token next() {
        // skip whitespace
        while (Character.isWhitespace(nextChar) && !reachedEOF) getNextChar();

        Token t = new Token(line, col);

        if (reachedEOF) {
            t.setKind(TokenKind.EOF);
            lastToken = t;
            return t;
        }

        if (Character.isLetter(nextChar)) {
            readName(t);
        } else if (Character.isDigit(nextChar)) {
            readNumber(t);
        } else if (nextChar == ';') {
            getNextChar();
            t.setKind(TokenKind.SEMICOLON);
        } else if (nextChar == '.') {
            getNextChar();
            t.setKind(TokenKind.PERIOD);
        } else if (nextChar == ',') {
            getNextChar();
            t.setKind(TokenKind.COMMA);
        } else if (nextChar == '*') {
            getNextChar();
            t.setKind(TokenKind.TIMES);
        } else if (nextChar == '%') {
            getNextChar();
            t.setKind(TokenKind.MODULO);
        } else if (nextChar == '(') {
            getNextChar();
            t.setKind(TokenKind.LEFT_PARENS);
        } else if (nextChar == ')') {
            getNextChar();
            t.setKind(TokenKind.RIGHT_PARENS);
        } else if (nextChar == '[') {
            getNextChar();
            t.setKind(TokenKind.LEFT_BRACKET);
        } else if (nextChar == ']') {
            getNextChar();
            t.setKind(TokenKind.RIGHT_BRACKET);
        } else if (nextChar == '{') {
            getNextChar();
            t.setKind(TokenKind.LEFT_BRACE);
        } else if (nextChar == '}') {
            getNextChar();
            t.setKind(TokenKind.RIGHT_BRACE);
        } else if (nextChar == '=') {
            getNextChar();

            if (nextChar == '=') {
                getNextChar();
                t.setKind(TokenKind.EQUALS);
            } else {
                t.setKind(TokenKind.ASSIGN);
            }
        } else if (nextChar == '&') {
            getNextChar();

            if (!reachedEOF && nextChar == '&') {
                getNextChar();
                t.setKind(TokenKind.AND);
            } else {
                t.setKind(TokenKind.INVALID);
            }
        } else if (nextChar == '|') {
            getNextChar();

            if (!reachedEOF && nextChar == '|') {
                getNextChar();
                t.setKind(TokenKind.OR);
            } else {
                t.setKind(TokenKind.INVALID);
            }
        } else if (nextChar == '+') {
            getNextChar();

            if (!reachedEOF && nextChar == '+') {
                getNextChar();
                t.setKind(TokenKind.PLUS_PLUS);
            } else {
                t.setKind(TokenKind.PLUS);
            }
        } else if (nextChar == '-') {
            getNextChar();

            if (!reachedEOF && nextChar == '-') {
                getNextChar();
                t.setKind(TokenKind.MINUS_MINUS);
            } else {
                t.setKind(TokenKind.MINUS);
            }
        } else if (nextChar == '!') {
            getNextChar();

            if (!reachedEOF && nextChar == '=') {
                getNextChar();
                t.setKind(TokenKind.NOT_EQUALS);
            } else {
                t.setKind(TokenKind.INVALID);
            }
        } else if (nextChar == '<') {
            getNextChar();

            if (!reachedEOF && nextChar == '=') {
                getNextChar();
                t.setKind(TokenKind.LESS);
            } else {
                t.setKind(TokenKind.LESS_EQUAL);
            }
        } else if (nextChar == '>') {
            getNextChar();

            if (!reachedEOF && nextChar == '=') {
                getNextChar();
                t.setKind(TokenKind.GREATER);
            } else {
                t.setKind(TokenKind.GREATER_EQUAL);
            }
        } else if (nextChar == '\'') {
            getNextChar();

            if (!reachedEOF && nextChar == '\'') {
                t.setKind(TokenKind.INVALID);
            } else {
                char c = nextChar;

                getNextChar();

                if (!reachedEOF && nextChar == '\'') {
                    getNextChar();

                    t.setKind(TokenKind.CHAR);
                    t.setValue(c);
                } else {
                    t.setKind(TokenKind.INVALID);
                }
            }
        } else if (nextChar == '/') {
            getNextChar();

            if (!reachedEOF && nextChar == '/') {
                do {
                    getNextChar();
                    // TODO: Deal with \r\n line separators
                } while (!reachedEOF && nextChar != '\n');

                t = next();
            } else {
                t.setKind(TokenKind.SLASH);
            }
        } else {
            getNextChar();
            t.setKind(TokenKind.INVALID);
        }

        lastToken = t;
        return t;
    }

    private void readName(Token t) {
        StringBuilder sb = new StringBuilder();
        sb.append(nextChar);

        do {
            getNextChar();
            sb.append(nextChar);
        } while (!reachedEOF && Character.isLetter(nextChar) || Character.isDigit(nextChar) || nextChar == '_');

        sb.deleteCharAt(sb.length() - 1);

        String value = sb.toString();

        TokenKind kind = switch (value) {
            case "break" -> TokenKind.BREAK;
            case "class" -> TokenKind.CLASS;
            case "else" -> TokenKind.ELSE;
            case "final" -> TokenKind.FINAL;
            case "if" -> TokenKind.IF;
            case "new" -> TokenKind.NEW;
            case "print" -> TokenKind.PRINT;
            case "program" -> TokenKind.PROGRAM;
            case "read" -> TokenKind.READ;
            case "return" -> TokenKind.RETURN;
            case "void" -> TokenKind.VOID;
            case "while" -> TokenKind.WHILE;
            default -> TokenKind.IDENTIFIER;
        };

        if (kind == TokenKind.IDENTIFIER) {
            t.setString(value);
        }

        t.setKind(kind);
    }

    private void readNumber(Token t) {
        StringBuilder sb = new StringBuilder();
        sb.append(nextChar);

        do {
            getNextChar();
            sb.append(nextChar);
        } while (!reachedEOF && Character.isDigit(nextChar));

        sb.deleteCharAt(sb.length() - 1);

        try {
            t.setValue(Integer.parseInt(sb.toString()));
        } catch (NumberFormatException ignored) {
            // TODO: error logging
            t.setKind(TokenKind.INVALID);
            return;
        }

        t.setKind(TokenKind.NUMBER);
    }

    private void getNextChar() {
        try {
            int in = input.read();
            col++;

            // EOF
            if (in == -1) {
                reachedEOF = true;
                return;
            }

            nextChar = (char) in;

            // TODO: Deal with \r\n line separators
            if (nextChar == '\n') {
                line++;
                col = 0;
            }
        } catch (IOException e) {
            // TODO: Handle exception
        }
    }
}
