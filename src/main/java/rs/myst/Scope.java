package rs.myst;

import lombok.Getter;

import java.util.LinkedList;

@Getter
public class Scope {
    private final LinkedList<Symbol> nodes = new LinkedList<>();

    private int numberOfVars = 0;

    public void addNode(Symbol node) {
        nodes.addLast(node);
    }

    public boolean existsByName(String name) {
        return findByName(name) != null;
    }

    public Symbol findByName(String name) {
        for (Symbol node : nodes) {
            if (node.getName().equals(name)) return node;
        }

        return null;
    }

    public int getNextAddress() {
        return numberOfVars++;
    }
}
