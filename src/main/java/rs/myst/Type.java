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

    private int sizeof = 4;

    private final LinkedList<Symbol> fields = new LinkedList<>();

    public Type(TypeKind kind) {
        this.kind = kind;

        if (kind == TypeKind.CHAR) sizeof = 1;
    }

    public Type(TypeKind kind, Type arrayElementType) {
        this.kind = kind;
        this.arrayElementType = arrayElementType;
    }

    public boolean isAssignableTo(Type that) {
        if (this.getActualKind() == that.getActualKind()) return true;

        return (this.getKind() == TypeKind.ARRAY || this.getKind() == TypeKind.CLASS) && that.getKind() == TypeKind.NONE;
    }

    public boolean isComparable() {
        return getActualKind() == TypeKind.CHAR || getActualKind() == TypeKind.INT;
    }

    public boolean isArithmetic() {
        return getActualKind() == TypeKind.INT;
    }

    public TypeKind getActualKind() {
        return kind == TypeKind.ARRAY ? arrayElementType.kind : kind;
    }

    @Override
    public String toString() {
        if (kind == TypeKind.ARRAY) {
            return arrayElementType.kind + "[]";
        } else {
            return kind.toString();
        }
    }
}
