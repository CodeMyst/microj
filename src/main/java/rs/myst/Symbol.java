package rs.myst;

import lombok.*;

import java.util.LinkedList;
import java.util.List;

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

    private final LinkedList<Symbol> locals = new LinkedList<>();

    public void addLocalSymbol(Symbol local) {
        locals.addLast(local);
    }

    public void addLocals(List<Symbol> locals) {
        this.locals.addAll(locals);
    }

    public boolean existsLocal(String name) {
        for (Symbol s : locals) if (s.getName().equals(name)) return true;

        return false;
    }
}
