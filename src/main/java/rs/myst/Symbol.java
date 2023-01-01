package rs.myst;

import lombok.*;

import java.util.LinkedList;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Symbol {
    private SymbolKind kind;
    private String name;
    private Type type;
    private int value;
    private int address;
    private ScopeType scopeType;
    private int numberOfParams;
    private LinkedList<Symbol> locals = new LinkedList<>();

    public void addLocalSymbol(Symbol local) {
        if (locals == null) locals = new LinkedList<>();
        locals.addLast(local);
    }

    public boolean existsLocal(String name) {
        if (locals == null) return false;

        for (Symbol s : locals) if (s.getName().equals(name)) return true;

        return false;
    }
}
