package rs.myst;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
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
    private boolean insideLoop = false;

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
            currentMethod.setKind(SymbolKind.METHOD);

            symbolTable.insert(currentMethod);
        }

        scan();

        symbolTable.openScope();

        check(LEFT_PARENS);
        if (next(IDENTIFIER)) {
            LinkedList<Symbol> params = formParams();
            if (valid) currentMethod.addLocals(params);
        }
        check(RIGHT_PARENS);

        while (next(IDENTIFIER)) {
            varDeclaration();
        }

        block();

        symbolTable.closeScope();

        currentMethod = null;
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

            if (symbol == null) {
                error("Cannot redeclare symbol with the name " + nextToken.getString());
            }
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

                if (symbol == null) {
                    error("Cannot redeclare symbol with the name " + nextToken.getString());
                }
            }

            scan();
        }

        return params;
    }

    private Symbol designator() {
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

                        if (typeSymbol == null) {
                            error(symbol.getType().getName() + " doesn't exist in the current scope.");
                        } else {
                            symbol = typeSymbol.getLocal(nextToken.getString());

                            if (symbol == null) {
                                error("identifier " + nextToken.getString() + " doesn't exist on type " + typeSymbol.getName());
                            }
                        }
                    }
                } else {
                    error(IDENTIFIER, nextToken.getKind());
                }

                scan();
            } else {
                check(LEFT_BRACKET);

                Symbol expression = expression();
                if (expression.getType().getKind() != TypeKind.INT) {
                    error("Can't use type " + expression.getType().getKind() + " as an array size.");
                }

                check(RIGHT_BRACKET);
            }
        }

        return symbol;
    }

    private Type type() {
        Type type = null;

        if (next(IDENTIFIER)) {
            Symbol typeSymbol = symbolTable.findByName(nextToken.getString());

            if (typeSymbol != null && typeSymbol.getKind() == SymbolKind.TYPE) {
                type = typeSymbol.getType();
            } else {
                error("Cannot resolve typename " + nextToken.getString());
            }
        } else {
            error("Expected type, got " + nextToken.getKind());
        }

        scan();

        if (next(LEFT_BRACKET)) {
            scan();

            type = new Type(TypeKind.ARRAY, type);

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
            Symbol symbol = designator();

            if (next(ASSIGN)) {
                scan();

                Symbol expression = expression();

                if (symbol.getKind() != SymbolKind.VARIABLE) {
                    error("Cannot assign to " + symbol.getKind());
                }

                boolean valid = true;

                if (symbol.getType().getKind() != TypeKind.ARRAY) {
                    if (expression.getType().getKind() != TypeKind.ARRAY) {
                        if (symbol.getType().getKind() != expression.getType().getKind()) {
                            valid = false;
                        }
                    } else {
                        if (symbol.getType().getKind() != expression.getType().getArrayElementType().getKind()) {
                            valid = false;
                        }
                    }
                } else {
                    if (expression.getType().getKind() == TypeKind.ARRAY) {
                        if (symbol.getType().getArrayElementType() != expression.getType().getArrayElementType()) {
                            valid = false;
                        }
                    } else {
                        if (symbol.getType().getArrayElementType().getKind() != expression.getType().getKind()) {
                            valid = false;
                        }
                    }
                }

                if (!valid) {
                    error("Cannot assign " + expression.getType() + " to " + symbol.getType());
                }

                check(SEMICOLON);
            } else if (next(LEFT_PARENS)) {
                scan();

                if (next(MINUS) ||
                    next(IDENTIFIER) ||
                    next(NUMBER) ||
                    next(CHAR) ||
                    next(NEW) ||
                    next(LEFT_PARENS)) {

                    List<Symbol> params = actParams();

                    if (symbol != null) {
                        if (params.size() != symbol.getNumberOfParams()) {
                            error("Method " + symbol.getName() + " accepts " + symbol.getNumberOfParams() + " parameters, but " + params.size() + " were provided");
                        } else {
                            for (int i = 0; i < params.size(); i++) {
                                TypeKind provided = params.get(i).getType().getKind();

                                if (symbol.getLocals().get(i) == null) continue;

                                TypeKind expected = symbol.getLocals().get(i).getType().getKind();
                                if (provided != expected) {
                                    error("Method " + symbol.getName() + " parameter " + (i+1) + " should be of type " + expected + " but " + provided + " was provided.");
                                }
                            }
                        }
                    }
                }

                check(RIGHT_PARENS);
                check(SEMICOLON);
            } else if (next(PLUS_PLUS)) {
                scan();

                if (symbol.getKind() != SymbolKind.VARIABLE) {
                    error("Cannot assign to a " + symbol.getKind());
                } else if (symbol.getType().getKind() != TypeKind.INT && (symbol.getType().getKind() == TypeKind.ARRAY && symbol.getType().getArrayElementType().getKind() != TypeKind.INT)) {
                    error("Cannot increment a " + symbol.getType().getKind());
                }

                check(SEMICOLON);
            } else {
                check(MINUS_MINUS);

                if (symbol.getKind() != SymbolKind.VARIABLE) {
                    error("Cannot assign to a " + symbol.getKind());
                } else if (symbol.getType().getKind() != TypeKind.INT && (symbol.getType().getKind() == TypeKind.ARRAY && symbol.getType().getArrayElementType().getKind() != TypeKind.INT)) {
                    error("Cannot decrement a " + symbol.getType().getKind());
                }

                check(SEMICOLON);
            }
        } else if (next(IF)) {
            scan();

            if (currentMethod == null) {
                error("Cannot use the if statement outside of methods.");
            }

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

            if (currentMethod == null) {
                error("Cannot use while loops outside of methods.");
            }

            check(LEFT_PARENS);
            condition();
            check(RIGHT_PARENS);

            insideLoop = true;
            statement();
            insideLoop = false;
        } else if (next(BREAK)) {
            scan();

            if (!insideLoop) {
                error("Cannot break outside of a loop.");
            }

            check(SEMICOLON);
        } else if (next(RETURN)) {
            scan();

            if (currentMethod == null) {
                error("Cannot use return outside of a method.");
            } else {
                if (next(SEMICOLON) && currentMethod.getType().getKind() != TypeKind.NONE) {
                    error("Return value of " + currentMethod.getType().getKind() + " expected.");
                }
            }

            if (next(MINUS) ||
                next(IDENTIFIER) ||
                next(NUMBER) ||
                next(CHAR) ||
                next(NEW) ||
                next(LEFT_PARENS)) {

                Symbol expression = expression();

                if (currentMethod != null) {
                    if (currentMethod.getType().getKind() != TypeKind.NONE) {
                        error("Return value not expected, method " + currentMethod.getName() + " returns void.");
                    } else {
                        if (currentMethod.getType().getKind() != expression.getType().getKind()) {
                            error("Return value of type " + currentMethod.getType().getKind() + " expected, got " + expression.getType().getKind());
                        }
                    }
                }
            }

            check(SEMICOLON);
        } else if (next(READ)) {
            scan();
            check(LEFT_PARENS);

            Symbol designator = designator();
            if (designator.getType().getKind() != TypeKind.INT && designator.getType().getKind() != TypeKind.CHAR) {
                error("Can't read " + designator.getType().getKind() + " from standard input.");
            }

            check(RIGHT_PARENS);
            check(SEMICOLON);
        } else if (next(PRINT)) {
            scan();
            check(LEFT_PARENS);

            Symbol expression = expression();
            if (expression.getType().getKind() != TypeKind.INT && expression.getType().getKind() != TypeKind.CHAR) {
                error("Can't output " + expression.getType().getKind() + " to standard output.");
            }

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

            if (symbol == null) {
                error("Cannot redeclare symbol with the name " + nextToken.getString());
            }
        }

        scan();

        while (next(COMMA)) {
            scan();

            if (!next(IDENTIFIER)) {
                error(IDENTIFIER, nextToken.getKind());
            } else {
                Symbol symbol = symbolTable.insert(SymbolKind.VARIABLE, nextToken.getString(), type);
                symbols.add(symbol);

                if (symbol == null) {
                    error("Cannot redeclare symbol with the name " + nextToken.getString());
                }
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

            symbolTable.insert(currentClass);

            if (currentClass == null) {
                error("Cannot redeclare symbol with the name " + nextToken.getString());
            } else {
                valid = true;
            }
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

        currentClass = null;
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

        Symbol e1 = expression();

        if (e1 != null) {
            Type t = e1.getType().getKind() == TypeKind.ARRAY ? e1.getType().getArrayElementType() : e1.getType();
            if (!(t.getKind() == TypeKind.CHAR || t.getKind() == TypeKind.INT)) {
                error("Only ints and chars can be compared.");
            }
        }

        relOp();

        Symbol e2 = expression();

        if (e2 != null) {
            Type t = e2.getType().getKind() == TypeKind.ARRAY ? e2.getType().getArrayElementType() : e2.getType();
            if (!(t.getKind() == TypeKind.CHAR || t.getKind() == TypeKind.INT)) {
                error("Only ints and chars can be compared.");
            }
        }
    }

    private Symbol expression() {
        // ["-"] term {addOp term}

        if (next(MINUS)) {
            scan();
        }

        Symbol t1 = term();

        if (t1 != null && (next(PLUS) || next(MINUS))) {
            Type t = t1.getType().getKind() == TypeKind.ARRAY ? t1.getType().getArrayElementType() : t1.getType();
            if (t.getKind() != TypeKind.INT) {
                error("Can't do math with " + t.getKind() + ", math can only be done with ints.");
            }
        }

        while (next(PLUS) || next(MINUS)) {
            addOp();

            Symbol t2 = term();

            if (t2 != null) {
                Type t = t2.getType().getKind() == TypeKind.ARRAY ? t2.getType().getArrayElementType() : t2.getType();
                if (t.getKind() != TypeKind.INT) {
                    error("Can't do math with " + t.getKind() + ", math can only be done with ints.");
                }
            }
        }

        return t1;
    }

    private Symbol term() {
        // factor {mulOp factor}

        Symbol f1 = factor();

        if (f1 != null && (next(TIMES) || next(SLASH) || next(MODULO))) {
            Type t = f1.getType().getKind() == TypeKind.ARRAY ? f1.getType().getArrayElementType() : f1.getType();
            if (t.getKind() != TypeKind.INT) {
                error("Can't do math with " + t.getKind() + ", math can only be done with ints.");
            }
        }

        while (next(TIMES) || next(SLASH) || next(MODULO)) {
            mulOp();

            Symbol f2 = factor();

            if (f2 != null) {
                Type t = f2.getType().getKind() == TypeKind.ARRAY ? f2.getType().getArrayElementType() : f2.getType();
                if (t.getKind() != TypeKind.INT) {
                    error("Can't do math with " + t.getKind() + ", math can only be done with ints.");
                }
            }
        }

        return f1;
    }

    private Symbol factor() {
        // designator ["(" [actParams] ")"] | number | charConst | "new" identifier ["[" expression "]"] | "(" expression ")"

        Symbol symbol = new Symbol();

        if (next(IDENTIFIER)) {
            symbol = designator();

            if (next(LEFT_PARENS)) {
                scan();

                if (symbol != null && !symbol.getKind().equals(SymbolKind.METHOD)) {
                    error("Can't call " + symbol.getName() + " as a method.");
                }

                if (next(MINUS) ||
                    next(IDENTIFIER) ||
                    next(NUMBER) ||
                    next(CHAR) ||
                    next(NEW) ||
                    next(LEFT_PARENS)) {

                    List<Symbol> params = actParams();

                    if (symbol != null) {
                        if (params.size() != symbol.getNumberOfParams()) {
                            error("Method " + symbol.getName() + " accepts " + symbol.getNumberOfParams() + " parameters, but " + params.size() + " were provided");
                        } else {
                            for (int i = 0; i < params.size(); i++) {
                                TypeKind provided = params.get(i).getType().getKind();

                                if (symbol.getLocals().get(i) == null) continue;

                                TypeKind expected = symbol.getLocals().get(i).getType().getKind();
                                if (provided != expected) {
                                    error("Method " + symbol.getName() + " parameter " + (i+1) + " should be of type " + expected + " but " + provided + " was provided.");
                                }
                            }
                        }
                    }
                }

                check(RIGHT_PARENS);
            }
        } else if (next(NUMBER)) {
            scan();

            symbol.setType(new Type(TypeKind.INT));
            symbol.setKind(SymbolKind.CONSTANT);
        } else if (next(CHAR)) {
            scan();

            symbol.setType(new Type(TypeKind.CHAR));
            symbol.setKind(SymbolKind.CONSTANT);
        } else if (next(NEW)) {
            scan();

            if (next(IDENTIFIER)) {
                symbol = symbolTable.findByName(nextToken.getString());
                if (symbol == null) {
                    error(nextToken.getString() + " doesn't exist in the current scope.");
                    symbol = new Symbol();
                } else if (symbol.getKind() != SymbolKind.TYPE) {
                    error(nextToken.getString() + " isn't a valid type.");
                }
            }
            check(IDENTIFIER);

            if (next(LEFT_BRACKET)) {
                scan();

                Symbol expression = expression();
                if (expression.getType().getKind() != TypeKind.INT) {
                    error("Can't use type " + expression.getType().getKind() + " as an array size.");
                }

                check(RIGHT_BRACKET);
            }
        } else if (next(LEFT_PARENS)) {
            scan();
            symbol = expression();
            check(RIGHT_PARENS);
        } else {
            error("unexpected token: " + nextToken.getKind());
        }

        return symbol;
    }

    private List<Symbol> actParams() {
        // expression {"," expression}

        List<Symbol> symbols = new LinkedList<>();

        symbols.add(expression());

        while (next(COMMA)) {
            scan();
            symbols.add(expression());
        }

        return symbols;
    }
}
