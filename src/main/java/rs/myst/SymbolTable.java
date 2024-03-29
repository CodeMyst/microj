package rs.myst;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;

@Getter
@Setter
public class SymbolTable {
    private final LinkedList<Scope> scopes = new LinkedList<>();

    private int nextGlobalAddress = 0;
    private int nextLocalAddress = 0;

    private boolean nextScopeIsLoop;

    public SymbolTable() {
        openScope();
        generateBuiltinTypes();
    }

    public Symbol insert(SymbolKind kind, String name, Type type) {
        Symbol symbol = Symbol.builder()
                .kind(kind)
                .name(name)
                .type(type)
                .build();

        return insert(symbol);
    }

    public Symbol insert(Symbol symbol) {
        if (scopes.size() == 0) return null;

        if (existsByName(symbol.getName())) return null;

        final Scope scope = scopes.getLast();

        if (symbol.getKind() == SymbolKind.VARIABLE) {
            symbol.setAddress(symbol.getScopeType() == ScopeType.GLOBAL ? nextGlobalAddress++ : nextLocalAddress++);
        }

        symbol.setScopeType(scopes.size() == 2 ? ScopeType.GLOBAL : ScopeType.LOCAL);

        scope.addNode(symbol);

        return symbol;
    }

    public Symbol findByName(String name) {
        for (Scope scope : scopes) {
            Symbol node = scope.findByName(name);
            if (node != null) return node;
        }

        return null;
    }

    public int numberOfLocals() {
        return scopes.getLast().getNodes().size();
    }

    public void openScope() {
        Scope newS = new Scope();
        newS.setLoop(nextScopeIsLoop);
        nextScopeIsLoop = false;
        scopes.addLast(newS);
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
        insert(SymbolKind.TYPE, "int", new Type(TypeKind.INT));
        insert(SymbolKind.TYPE, "char", new Type(TypeKind.CHAR));

        Symbol chrSymbol = Symbol.builder()
                .kind(SymbolKind.METHOD)
                .name("chr")
                .type(new Type(TypeKind.CHAR))
                .build();

        chrSymbol.addLocalSymbol(Symbol.builder()
                .kind(SymbolKind.VARIABLE)
                .name("i")
                .type(new Type(TypeKind.INT))
                .scopeType(ScopeType.LOCAL)
                .build());

        insert(chrSymbol);

        Symbol ordSymbol = Symbol.builder()
                .kind(SymbolKind.METHOD)
                .name("ord")
                .type(new Type(TypeKind.INT))
                .build();

        ordSymbol.addLocalSymbol(Symbol.builder()
                .kind(SymbolKind.VARIABLE)
                .name("ch")
                .type(new Type(TypeKind.CHAR))
                .scopeType(ScopeType.LOCAL)
                .build());

        insert(ordSymbol);

        Symbol lenSymbol = Symbol.builder()
                .kind(SymbolKind.METHOD)
                .name("len")
                .type(new Type(TypeKind.INT))
                .build();

        lenSymbol.addLocalSymbol(Symbol.builder()
                .kind(SymbolKind.VARIABLE)
                .name("arr")
                .type(new Type(TypeKind.ARRAY, new Type(TypeKind.NONE)))
                .scopeType(ScopeType.LOCAL)
                .build());

        insert(lenSymbol);
    }
}
