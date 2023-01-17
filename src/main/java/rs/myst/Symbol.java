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
    @Builder.Default
    private SymbolKind kind = SymbolKind.NONE;
    private String name;
    private Type type;
    private int value;
    private int address;
    private ScopeType scopeType;

    private final LinkedList<Symbol> locals = new LinkedList<>();

    public void addLocalSymbol(Symbol local) {
        locals.addLast(local);
    }

    public void addLocals(List<Symbol> locals) {
        this.locals.addAll(locals);
    }

    public Symbol getLocal(String name) {
        for (Symbol s : locals) if (s.getName().equals(name)) return s;

        return null;
    }

    public int getNumberOfParams() {
        return locals.size();
    }
}
