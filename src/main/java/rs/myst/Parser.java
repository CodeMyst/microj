package rs.myst;

public class Parser {
    private final Scanner scanner;

    private Token nextToken;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    public void parse() {
        scan();

        program();

        check(TokenKind.EOF);
    }

    private void scan() {
        // TODO: Check if token valid
        nextToken = scanner.next();
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

    private void error(String message) {
        // TODO: Better error handling
        System.out.println("line: " + nextToken.getLine() + ", col: " + nextToken.getCol() + ": " + message);

        System.exit(1);
    }

    private void program() {
        // "program" identifier {constDeclaration | varDeclaration | classDeclaration} "{" {methodDeclaration} "}"

        check(TokenKind.PROGRAM);
        check(TokenKind.IDENTIFIER);

        while (next(TokenKind.FINAL) || next(TokenKind.IDENTIFIER) || next(TokenKind.CLASS)) {
            if (next(TokenKind.FINAL)) {
                constDeclaration();
            } else if (next(TokenKind.IDENTIFIER)) {
                varDeclaration();
            } else {
                classDeclaration();
            }
        }

        check(TokenKind.LEFT_BRACE);
        while (next(TokenKind.IDENTIFIER) || next(TokenKind.VOID)) {
            methodDeclaration();
        }
        check(TokenKind.RIGHT_BRACE);
    }

    private void methodDeclaration() {
        // (type | "void") identifier "(" [formParams] ")" {varDeclaration} block

        if (next(TokenKind.VOID)) {
            scan();
        } else {
            type();
        }

        check(TokenKind.IDENTIFIER);

        check(TokenKind.LEFT_PARENS);
        // should be a type
        if (next(TokenKind.IDENTIFIER)) {
            formParams();
        }
        check(TokenKind.RIGHT_PARENS);

        // should be a type
        while (next(TokenKind.IDENTIFIER)) {
            varDeclaration();
        }

        block();
    }

    private void formParams() {
        // type identifier {"," type identifier}

        type();
        check(TokenKind.IDENTIFIER);

        while (next(TokenKind.COMMA)) {
            scan();
            type();
            check(TokenKind.IDENTIFIER);
        }
    }

    private void designator() {
        // identifier {"." identifier | "[" Expression "]"}

        check(TokenKind.IDENTIFIER);

        while (next(TokenKind.PERIOD) || next(TokenKind.LEFT_BRACKET)) {
            if (next(TokenKind.PERIOD)) {
                scan();
                check(TokenKind.IDENTIFIER);
            } else {
                check(TokenKind.LEFT_BRACKET);
                expression();
                check(TokenKind.RIGHT_BRACKET);
            }
        }
    }

    private void type() {
        check(TokenKind.IDENTIFIER);

        if (next(TokenKind.LEFT_BRACKET)) {
            scan();
            check(TokenKind.RIGHT_BRACKET);
        }
    }

    private void block() {
        // "{" {statement} "}"

        check(TokenKind.LEFT_BRACE);

        while (next(TokenKind.IDENTIFIER) ||
               next(TokenKind.IF) ||
               next(TokenKind.WHILE) ||
               next(TokenKind.BREAK) ||
               next(TokenKind.RETURN) ||
               next(TokenKind.READ) ||
               next(TokenKind.PRINT) ||
               next(TokenKind.LEFT_BRACE) ||
               next(TokenKind.SEMICOLON)) {

            statement();
        }

        check(TokenKind.RIGHT_BRACE);
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

        if (next(TokenKind.IDENTIFIER)) {
            designator();

            if (next(TokenKind.ASSIGN)) {
                scan();
                expression();
                check(TokenKind.SEMICOLON);
            } else if (next(TokenKind.LEFT_PARENS)) {
                scan();

                if (next(TokenKind.MINUS) ||
                    next(TokenKind.IDENTIFIER) ||
                    next(TokenKind.NUMBER) ||
                    next(TokenKind.CHAR) ||
                    next(TokenKind.NEW) ||
                    next(TokenKind.LEFT_PARENS)) {

                    actParams();
                }

                check(TokenKind.RIGHT_PARENS);
                check(TokenKind.SEMICOLON);
            } else if (next(TokenKind.PLUS_PLUS)) {
                scan();
                check(TokenKind.SEMICOLON);
            } else {
                check(TokenKind.MINUS_MINUS);
                check(TokenKind.SEMICOLON);
            }
        } else if (next(TokenKind.IF)) {
            scan();
            check(TokenKind.LEFT_PARENS);
            condition();
            check(TokenKind.RIGHT_PARENS);

            statement();

            if (next(TokenKind.ELSE)) {
                scan();
                statement();
            }
        } else if (next(TokenKind.WHILE)) {
            scan();
            check(TokenKind.LEFT_PARENS);
            condition();
            check(TokenKind.RIGHT_PARENS);

            statement();
        } else if (next(TokenKind.BREAK)) {
            scan();
            check(TokenKind.SEMICOLON);
        } else if (next(TokenKind.RETURN)) {
            scan();

            if (next(TokenKind.MINUS) ||
                next(TokenKind.IDENTIFIER) ||
                next(TokenKind.NUMBER) ||
                next(TokenKind.CHAR) ||
                next(TokenKind.NEW) ||
                next(TokenKind.LEFT_PARENS)) {

                expression();
            }

            check(TokenKind.SEMICOLON);
        } else if (next(TokenKind.READ)) {
            scan();
            check(TokenKind.LEFT_PARENS);
            designator();
            check(TokenKind.RIGHT_PARENS);
            check(TokenKind.SEMICOLON);
        } else if (next(TokenKind.PRINT)) {
            scan();
            check(TokenKind.LEFT_PARENS);

            expression();

            if (next(TokenKind.COMMA)) {
                scan();
                check(TokenKind.NUMBER);
            }

            check(TokenKind.RIGHT_PARENS);
            check(TokenKind.SEMICOLON);
        } else if (next(TokenKind.IDENTIFIER) ||
                   next(TokenKind.IF) ||
                   next(TokenKind.WHILE) ||
                   next(TokenKind.BREAK) ||
                   next(TokenKind.RETURN) ||
                   next(TokenKind.READ) ||
                   next(TokenKind.PRINT) ||
                   next(TokenKind.LEFT_BRACE) ||
                   next(TokenKind.SEMICOLON)) {
            block();
        } else {
            check(TokenKind.SEMICOLON);
        }
    }

    private void constDeclaration() {
        // "final" type identifier "=" (number | charConst) ";"

        check(TokenKind.FINAL);
        type();
        check(TokenKind.IDENTIFIER);
        check(TokenKind.ASSIGN);

        if (next(TokenKind.NUMBER)) {
            scan();
        } else {
            check(TokenKind.CHAR);
        }

        check(TokenKind.SEMICOLON);
    }

    private void varDeclaration() {
        // type identifier {"," identifier} ";"

        type();
        check(TokenKind.IDENTIFIER);

        while (next(TokenKind.COMMA)) {
            scan();
            check(TokenKind.IDENTIFIER);
        }

        check(TokenKind.SEMICOLON);
    }

    private void classDeclaration() {
        // "class" identifier "{" {varDeclaration} "}"

        check(TokenKind.CLASS);
        check(TokenKind.IDENTIFIER);
        check(TokenKind.LEFT_BRACE);

        while (next(TokenKind.IDENTIFIER)) {
            varDeclaration();
        }

        check(TokenKind.RIGHT_BRACE);
    }

    private void relOp() {
        // "==" | "!=" | ">" | ">=" | "<" | "<="

        if (next(TokenKind.EQUALS)) {
            scan();
        } else if (next(TokenKind.NOT_EQUALS)) {
            scan();
        } else if (next(TokenKind.GREATER)) {
            scan();
        } else if (next(TokenKind.GREATER_EQUAL)) {
            scan();
        } else if (next(TokenKind.LESS)) {
            scan();
        } else {
            check(TokenKind.LESS_EQUAL);
        }
    }

    private void addOp() {
        // "+" | "-"

        if (next(TokenKind.PLUS)) {
            scan();
        } else {
            check(TokenKind.MINUS);
        }
    }

    private void mulOp() {
        // "*" | "/" | "%"

        if (next(TokenKind.TIMES)) {
            scan();
        } else if (next(TokenKind.SLASH)) {
            scan();
        } else {
            check(TokenKind.MODULO);
        }
    }

    private void condition() {
        // conditionTerm {"||" conditionTerm}

        conditionTerm();

        while (next(TokenKind.OR)) {
            scan();
            conditionTerm();
        }
    }

    private void conditionTerm() {
        // conditionFact {"&&" conditionFact}

        conditionFact();

        while (next(TokenKind.AND)) {
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

        if (next(TokenKind.MINUS)) {
            scan();
        }

        term();

        while (next(TokenKind.PLUS) || next(TokenKind.MINUS)) {
            addOp();

            term();
        }
    }

    private void term() {
        // factor {mulOp factor}

        factor();

        while (next(TokenKind.TIMES) || next(TokenKind.SLASH) || next(TokenKind.MODULO)) {
            mulOp();

            factor();
        }
    }

    private void factor() {
        // designator ["(" [actParams] ")"] | number | charConst | "new" identifier ["[" expression "]"] | "(" expression ")"

        if (next(TokenKind.IDENTIFIER)) {
            designator();

            if (next(TokenKind.LEFT_PARENS)) {
                scan();

                if (next(TokenKind.MINUS) ||
                    next(TokenKind.IDENTIFIER) ||
                    next(TokenKind.NUMBER) ||
                    next(TokenKind.CHAR) ||
                    next(TokenKind.NEW) ||
                    next(TokenKind.LEFT_PARENS)) {

                    actParams();
                }

                check(TokenKind.RIGHT_PARENS);
            }
        } else if (next(TokenKind.NUMBER)) {
            scan();
        } else if (next(TokenKind.CHAR)) {
            scan();
        } else if (next(TokenKind.NEW)) {
            scan();
            check(TokenKind.IDENTIFIER);

            if (next(TokenKind.LEFT_BRACKET)) {
                scan();
                expression();
                check(TokenKind.RIGHT_BRACKET);
            }
        } else {
            check(TokenKind.LEFT_PARENS);
            expression();
            check(TokenKind.RIGHT_PARENS);
        }
    }

    private void actParams() {
        // expression {"," expression}

        expression();

        while (next(TokenKind.COMMA)) {
            expression();
        }
    }
}
