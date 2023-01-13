package rs.myst;

import java.util.EnumSet;
import java.util.LinkedList;
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

    private final SymbolTable symbolTable = new SymbolTable();
    private Symbol currentMethod = null;
    private Symbol currentClass = null;

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
            error(expected, nextToken.getKind());
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

    private void error(TokenKind expected, TokenKind got) {
        error("expected " + expected + ", got " + got);
    }

    private void program() {
        // "program" identifier {constDeclaration | varDeclaration | classDeclaration} "{" {methodDeclaration} "}"

        check(PROGRAM);
        check(IDENTIFIER);

        symbolTable.openScope();

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

        symbolTable.closeScope();
    }

    private void methodDeclaration() {
        // (type | "void") identifier "(" [formParams] ")" {varDeclaration} block

        Type type;

        if (next(VOID)) {
            scan();
            type = new Type(TypeKind.NONE);
        } else {
            type = type();
        }

        boolean valid = true;

        if (!next(IDENTIFIER)) {
            error(IDENTIFIER, nextToken.getKind());
            valid = false;
        } else {
            currentMethod = new Symbol();
            currentMethod.setName(nextToken.getString());
            currentMethod.setType(type);

            symbolTable.insert(currentMethod);
        }

        scan();

        symbolTable.openScope();

        check(LEFT_PARENS);
        // should be a type
        if (next(IDENTIFIER)) {
            LinkedList<Symbol> params = formParams();
            if (valid) currentMethod.addLocals(params);
        }
        check(RIGHT_PARENS);

        // should be a type
        while (next(IDENTIFIER)) {
            LinkedList<Symbol> vars = varDeclaration();
            if (valid) currentMethod.addLocals(vars);
        }

        block();

        symbolTable.closeScope();
    }

    private LinkedList<Symbol> formParams() {
        // type identifier {"," type identifier}

        LinkedList<Symbol> params = new LinkedList<>();

        Type type = type();

        if (!next(IDENTIFIER)) {
            error(IDENTIFIER, nextToken.getKind());
        } else {
            Symbol symbol = symbolTable.insert(SymbolKind.VARIABLE, nextToken.getString(), type);
            params.add(symbol);
        }

        scan();

        while (next(COMMA)) {
            scan();

            type = type();

            if (!next(IDENTIFIER)) {
                error(IDENTIFIER, nextToken.getKind());
            } else {
                Symbol symbol = symbolTable.insert(SymbolKind.VARIABLE, nextToken.getString(), type);
                params.add(symbol);
            }

            scan();
        }

        return params;
    }

    private void designator() {
        // identifier {"." identifier | "[" Expression "]"}

        Symbol symbol = null;

        if (next(IDENTIFIER)) {
            symbol = symbolTable.findByName(nextToken.getString());

            if (symbol == null) {
                error("failed to resolve typename " + nextToken.getString());
            }
        } else {
            error(IDENTIFIER, nextToken.getKind());
        }

        scan();

        while (next(PERIOD) || next(LEFT_BRACKET)) {
            if (next(PERIOD)) {
                scan();

                if (next(IDENTIFIER)) {
                    if (symbol != null) {
                        Symbol typeSymbol = symbolTable.findByName(symbol.getType().getName());

                        if (!typeSymbol.existsLocal(nextToken.getString())) {
                            error("identifier " + nextToken.getString() + " doesn't exist on type " + symbol.getType().getName());
                        }
                    }
                } else {
                    error(IDENTIFIER, nextToken.getKind());
                }

                scan();
            } else {
                check(LEFT_BRACKET);
                expression();
                check(RIGHT_BRACKET);
            }
        }
    }

    private Type type() {
        Type type = null;

        if (next(IDENTIFIER)) {
            Symbol typeSymbol = symbolTable.findByName(nextToken.getString());

            if (typeSymbol != null && typeSymbol.getKind() == SymbolKind.TYPE) {
                type = typeSymbol.getType();
            } else {
                error("cannot resolve typename " + nextToken.getString());
            }
        } else {
            error("expected type, got " + nextToken.getKind());
        }

        scan();

        if (next(LEFT_BRACKET)) {
            scan();
            check(RIGHT_BRACKET);
        }

        return type;
    }

    private void block() {
        // "{" {statement} "}"

        check(LEFT_BRACE);

        symbolTable.openScope();

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

        symbolTable.closeScope();

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

        Type type = type();

        if (type.getKind() != TypeKind.INT && type.getKind() != TypeKind.CHAR) {
            error("cannot declare a constant of type: " + type.getKind());
        } else if (!next(IDENTIFIER)) {
            error(IDENTIFIER, nextToken.getKind());
        } else {
            symbolTable.insert(SymbolKind.CONSTANT, nextToken.getString(), type);
        }

        scan();
        check(ASSIGN);

        if (next(NUMBER)) {
            scan();
        } else {
            check(CHAR);
        }

        check(SEMICOLON);
    }

    private LinkedList<Symbol> varDeclaration() {
        // type identifier {"," identifier} ";"

        LinkedList<Symbol> symbols = new LinkedList<>();

        Type type = type();

        if (!next(IDENTIFIER)) {
            error(IDENTIFIER, nextToken.getKind());
        } else {
            Symbol symbol = symbolTable.insert(SymbolKind.VARIABLE, nextToken.getString(), type);
            symbols.add(symbol);
        }

        scan();

        while (next(COMMA)) {
            scan();

            if (!next(IDENTIFIER)) {
                error(IDENTIFIER, nextToken.getKind());
            } else {
                Symbol symbol = symbolTable.insert(SymbolKind.VARIABLE, nextToken.getString(), type);
                symbols.add(symbol);
            }

            scan();
        }

        check(SEMICOLON);

        return symbols;
    }

    private void classDeclaration() {
        // "class" identifier "{" {varDeclaration} "}"

        check(CLASS);

        boolean valid = false;

        if (!next(IDENTIFIER)) {
            error(IDENTIFIER, nextToken.getKind());
        } else {
            Type type = new Type(TypeKind.CLASS);
            type.setName(nextToken.getString());
            currentClass = symbolTable.insert(SymbolKind.TYPE, nextToken.getString(), type);
            valid = true;
        }

        scan();

        symbolTable.openScope();

        check(LEFT_BRACE);

        while (next(IDENTIFIER)) {
            LinkedList<Symbol> vars = varDeclaration();
            if (valid) currentClass.addLocals(vars);
        }

        check(RIGHT_BRACE);

        symbolTable.closeScope();
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
        } else if (next(LESS_EQUAL)) {
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
