package rs.myst;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;

@Getter
@Setter
public class Type {
    private TypeKind kind;

    private String name;

    private Type arrayElementType; // type of elements in the array (if this type is an array)

    private final LinkedList<Symbol> fields = new LinkedList<>();

    public Type(TypeKind kind) {
        this.kind = kind;
    }

    public Type(TypeKind kind, Type arrayElementType) {
        this.kind = kind;
        this.arrayElementType = arrayElementType;
    }
}
