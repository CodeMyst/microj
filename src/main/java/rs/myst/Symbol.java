package rs.myst;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;

@Getter
@Setter
@Builder
public class Symbol {
    private SymbolKind kind;
    private String name;
    private Type type;
    private int value;
    private int address;
    private ScopeType scopeType;
    private int numberOfParams;
    private final LinkedList<Symbol> locals = new LinkedList<>();

    public void addLocalSymbol(Symbol local) {
        locals.addLast(local);
    }
}
