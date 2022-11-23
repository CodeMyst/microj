package rs.myst;

import java.util.EnumSet;
import java.util.Set;

import static rs.myst.TokenKind.*;

public class Parser {
    // how far away from the last error token we have to be to report new errors
    private final int ERROR_DISTANCE_THRESHOLD = 3;

    private final Scanner scanner;

    private Token nextToken;

    private int errorCount = 0;
    // current error distance from the last errored token
    private int errorDistance = ERROR_DISTANCE_THRESHOLD;

    // list of all token kinds that a statement can start with
    // makes it easier to do checks
    private final Set<TokenKind> statementFirstTokens = EnumSet.of(
            IDENTIFIER,
            IF,
            WHILE,
            BREAK,
            RETURN,
            READ,
            PRINT,
            LEFT_BRACE,
            SEMICOLON);

    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    public void parse() {
        scan();

        program();

        check(EOF);
    }

    public int getErrorCount() {
        return errorCount;
    }

    public boolean parsedSuccessfully() {
        return errorCount == 0;
    }

    private void scan() {
        nextToken = scanner.next();
        errorDistance++;
    }

    private void check(TokenKind expected) {
        if (next(expected)) {
            scan();
        } else {
            error("expected " + expected + ", got " + nextToken.getKind());
        }
    }

    private boolean next(TokenKind kind) {
        return nextToken.getKind() == kind;
    }

    private boolean nextOf(Set<TokenKind> kinds) {
        return kinds.contains(nextToken.getKind());
    }

    private void error(String message) {
        if (errorDistance >= ERROR_DISTANCE_THRESHOLD) {
            System.out.println("line " + nextToken.getLine() + ", col " + nextToken.getCol() + ": " + message);
            errorCount++;
        }

        errorDistance = 0;
    }

    private void program() {
        // "program" identifier {constDeclaration | varDeclaration | classDeclaration} "{" {methodDeclaration} "}"

        check(PROGRAM);
        check(IDENTIFIER);

        while (next(FINAL) || next(IDENTIFIER) || next(CLASS)) {
            if (next(FINAL)) {
                constDeclaration();
            } else if (next(IDENTIFIER)) {
                varDeclaration();
            } else {
                classDeclaration();
            }
        }

        check(LEFT_BRACE);
        while (next(IDENTIFIER) || next(VOID)) {
            methodDeclaration();
        }
        check(RIGHT_BRACE);
    }

    private void methodDeclaration() {
        // (type | "void") identifier "(" [formParams] ")" {varDeclaration} block

        if (next(VOID)) {
            scan();
        } else {
            type();
        }

        check(IDENTIFIER);

        check(LEFT_PARENS);
        // should be a type
        if (next(IDENTIFIER)) {
            formParams();
        }
        check(RIGHT_PARENS);

        // should be a type
        while (next(IDENTIFIER)) {
            varDeclaration();
        }

        block();
    }

    private void formParams() {
        // type identifier {"," type identifier}

        type();
        check(IDENTIFIER);

        while (next(COMMA)) {
            scan();
            type();
            check(IDENTIFIER);
        }
    }

    private void designator() {
        // identifier {"." identifier | "[" Expression "]"}

        check(IDENTIFIER);

        while (next(PERIOD) || next(LEFT_BRACKET)) {
            if (next(PERIOD)) {
                scan();
                check(IDENTIFIER);
            } else {
                check(LEFT_BRACKET);
                expression();
                check(RIGHT_BRACKET);
            }
        }
    }

    private void type() {
        check(IDENTIFIER);

        if (next(LEFT_BRACKET)) {
            scan();
            check(RIGHT_BRACKET);
        }
    }

    private void block() {
        // "{" {statement} "}"

        check(LEFT_BRACE);

        while (true) {
            if (nextOf(statementFirstTokens)) {
                statement();
            } else if (next(RIGHT_BRACE) || next(EOF)) {
                break;
            } else {
                error("invalid start of a statement");
                do {
                    scan();
                } while (!nextOf(statementFirstTokens) && !next(EOF));
                errorDistance = 0;
            }
        }

        while (nextOf(statementFirstTokens)) {
            statement();
        }

        check(RIGHT_BRACE);
    }

    private void statement() {
        // designator ("=" expression ";" | "(" [actParams] ")" ";" | "++" ";" | "--" ";") |
        // "if" "(" condition ")" statement ["else" statement] |
        // "while" "(" condition ")" statement |
        // "break" ";" |
        // "return" [expression] ";" |
        // "read" "(" designator ")" ";" |
        // "print" "(" expression ["," number] ")" ";" |
        // block |
        // ";"

        if (next(IDENTIFIER)) {
            designator();

            if (next(ASSIGN)) {
                scan();
                expression();
                check(SEMICOLON);
            } else if (next(LEFT_PARENS)) {
                scan();

                if (next(MINUS) ||
                    next(IDENTIFIER) ||
                    next(NUMBER) ||
                    next(CHAR) ||
                    next(NEW) ||
                    next(LEFT_PARENS)) {

                    actParams();
                }

                check(RIGHT_PARENS);
                check(SEMICOLON);
            } else if (next(PLUS_PLUS)) {
                scan();
                check(SEMICOLON);
            } else {
                check(MINUS_MINUS);
                check(SEMICOLON);
            }
        } else if (next(IF)) {
            scan();
            check(LEFT_PARENS);
            condition();
            check(RIGHT_PARENS);

            statement();

            if (next(ELSE)) {
                scan();
                statement();
            }
        } else if (next(WHILE)) {
            scan();
            check(LEFT_PARENS);
            condition();
            check(RIGHT_PARENS);

            statement();
        } else if (next(BREAK)) {
            scan();
            check(SEMICOLON);
        } else if (next(RETURN)) {
            scan();

            if (next(MINUS) ||
                next(IDENTIFIER) ||
                next(NUMBER) ||
                next(CHAR) ||
                next(NEW) ||
                next(LEFT_PARENS)) {

                expression();
            }

            check(SEMICOLON);
        } else if (next(READ)) {
            scan();
            check(LEFT_PARENS);
            designator();
            check(RIGHT_PARENS);
            check(SEMICOLON);
        } else if (next(PRINT)) {
            scan();
            check(LEFT_PARENS);

            expression();

            if (next(COMMA)) {
                scan();
                check(NUMBER);
            }

            check(RIGHT_PARENS);
            check(SEMICOLON);
        } else if (next(LEFT_BRACE)) {
            block();
        } else {
            check(SEMICOLON);
        }
    }

    private void constDeclaration() {
        // "final" type identifier "=" (number | charConst) ";"

        check(FINAL);
        type();
        check(IDENTIFIER);
        check(ASSIGN);

        if (next(NUMBER)) {
            scan();
        } else {
            check(CHAR);
        }

        check(SEMICOLON);
    }

    private void varDeclaration() {
        // type identifier {"," identifier} ";"

        type();
        check(IDENTIFIER);

        while (next(COMMA)) {
            scan();
            check(IDENTIFIER);
        }

        check(SEMICOLON);
    }

    private void classDeclaration() {
        // "class" identifier "{" {varDeclaration} "}"

        check(CLASS);
        check(IDENTIFIER);
        check(LEFT_BRACE);

        while (next(IDENTIFIER)) {
            varDeclaration();
        }

        check(RIGHT_BRACE);
    }

    private void relOp() {
        // "==" | "!=" | ">" | ">=" | "<" | "<="

        if (next(EQUALS)) {
            scan();
        } else if (next(NOT_EQUALS)) {
            scan();
        } else if (next(GREATER)) {
            scan();
        } else if (next(GREATER_EQUAL)) {
            scan();
        } else if (next(LESS)) {
            scan();
        } else if (next(LESS)) {
            scan();
        } else {
            error("expected a relational operator, got: " + nextToken.getKind());
        }
    }

    private void addOp() {
        // "+" | "-"

        if (next(PLUS)) {
            scan();
        } else {
            check(MINUS);
        }
    }

    private void mulOp() {
        // "*" | "/" | "%"

        if (next(TIMES)) {
            scan();
        } else if (next(SLASH)) {
            scan();
        } else {
            check(MODULO);
        }
    }

    private void condition() {
        // conditionTerm {"||" conditionTerm}

        conditionTerm();

        while (next(OR)) {
            scan();
            conditionTerm();
        }
    }

    private void conditionTerm() {
        // conditionFact {"&&" conditionFact}

        conditionFact();

        while (next(AND)) {
            scan();
            conditionFact();
        }
    }

    private void conditionFact() {
        // expression relOp expression

        expression();

        relOp();

        expression();
    }

    private void expression() {
        // ["-"] term {addOp term}

        if (next(MINUS)) {
            scan();
        }

        term();

        while (next(PLUS) || next(MINUS)) {
            addOp();

            term();
        }
    }

    private void term() {
        // factor {mulOp factor}

        factor();

        while (next(TIMES) || next(SLASH) || next(MODULO)) {
            mulOp();

            factor();
        }
    }

    private void factor() {
        // designator ["(" [actParams] ")"] | number | charConst | "new" identifier ["[" expression "]"] | "(" expression ")"

        if (next(IDENTIFIER)) {
            designator();

            if (next(LEFT_PARENS)) {
                scan();

                if (next(MINUS) ||
                    next(IDENTIFIER) ||
                    next(NUMBER) ||
                    next(CHAR) ||
                    next(NEW) ||
                    next(LEFT_PARENS)) {

                    actParams();
                }

                check(RIGHT_PARENS);
            }
        } else if (next(NUMBER)) {
            scan();
        } else if (next(CHAR)) {
            scan();
        } else if (next(NEW)) {
            scan();
            check(IDENTIFIER);

            if (next(LEFT_BRACKET)) {
                scan();
                expression();
                check(RIGHT_BRACKET);
            }
        } else if (next(LEFT_PARENS)) {
            scan();
            expression();
            check(RIGHT_PARENS);
        } else {
            error("unexpected token: " + nextToken.getKind());
        }
    }

    private void actParams() {
        // expression {"," expression}

        expression();

        while (next(COMMA)) {
            expression();
        }
    }
}
