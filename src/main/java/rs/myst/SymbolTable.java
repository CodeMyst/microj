package rs.myst;

import java.util.LinkedList;

public class SymbolTable {
    public static final Type INT_TYPE = new Type(TypeKind.INT);
    public static final Type CHAR_TYPE = new Type(TypeKind.CHAR);
    public static final Type NULL_TYPE = new Type(TypeKind.CLASS);
    public static final Type NO_TYPE = new Type(TypeKind.NONE);

    public static Symbol CHR_SYMBOL;
    public static Symbol ORD_SYMBOL;
    public static Symbol LEN_SYMBOL;

    private final LinkedList<Scope> scopes = new LinkedList<>();

    public SymbolTable() {
        generateBuiltinTypes();
    }

    public Symbol insert(SymbolKind kind, String name, Type type) {
        if (scopes.size() == 0) return null;

        if (existsByName(name)) return null;

        final Symbol node = Symbol.builder()
                .kind(kind)
                .name(name)
                .type(type)
                .build();

        final Scope scope = scopes.getLast();

        if (kind == SymbolKind.VARIABLE) {
            node.setAddress(scope.getNextAddress());
        }

        node.setScopeType(scopes.size() == 1 ? ScopeType.GLOBAL : ScopeType.LOCAL);

        scope.addNode(node);

        return node;
    }

    public Symbol findByName(String name) {
        for (Scope scope : scopes) {
            Symbol node = scope.findByName(name);
            if (node != null) return node;
        }

        return null;
    }

    public void openScope() {
        scopes.addLast(new Scope());
    }

    public void closeScope() {
        scopes.removeLast();
    }

    private boolean existsByName(String name) {
        for (Scope scope : scopes) {
            if (scope.existsByName(name)) return true;
        }

        return false;
    }

    private void generateBuiltinTypes() {
        // TODO: add to scope

        CHR_SYMBOL = Symbol.builder()
                .kind(SymbolKind.METHOD)
                .name("chr")
                .type(CHAR_TYPE)
                .scopeType(ScopeType.GLOBAL)
                .numberOfParams(1)
                .build();

        CHR_SYMBOL.addLocalSymbol(Symbol.builder()
                .kind(SymbolKind.VARIABLE)
                .name("i")
                .type(INT_TYPE)
                .scopeType(ScopeType.LOCAL)
                .build());

        ORD_SYMBOL = Symbol.builder()
                .kind(SymbolKind.METHOD)
                .name("ord")
                .type(INT_TYPE)
                .scopeType(ScopeType.GLOBAL)
                .numberOfParams(1)
                .build();

        ORD_SYMBOL.addLocalSymbol(Symbol.builder()
                .kind(SymbolKind.VARIABLE)
                .name("ch")
                .type(CHAR_TYPE)
                .scopeType(ScopeType.LOCAL)
                .build());

        LEN_SYMBOL = Symbol.builder()
                .kind(SymbolKind.METHOD)
                .name("len")
                .type(INT_TYPE)
                .scopeType(ScopeType.GLOBAL)
                .numberOfParams(1)
                .build();

        LEN_SYMBOL.addLocalSymbol(Symbol.builder()
                .kind(SymbolKind.VARIABLE)
                .name("arr")
                .type(new Type(TypeKind.ARRAY, NO_TYPE))
                .scopeType(ScopeType.LOCAL)
                .build());
    }
}
