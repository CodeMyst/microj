package rs.myst;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Descriptor {
    private DescriptorKind kind;
    private Type type;
    private Symbol symbol;
    private Condition condition;
    private Label trueLabel;
    private Label falseLabel;

    private int value;
    private int address;

    public Descriptor(Symbol symbol) {
        type = symbol.getType();
        value = symbol.getValue();
        address = symbol.getAddress();
        this.symbol = symbol;

        switch (symbol.getKind()) {
            case CONSTANT -> kind = DescriptorKind.CONSTANT;

            case VARIABLE -> {
                if (symbol.getScopeType() == ScopeType.LOCAL) kind = DescriptorKind.LOCAL;
                else kind = DescriptorKind.STATIC;
            }

            case METHOD -> {
                kind = DescriptorKind.METHOD;
            }
        }
    }

    public Descriptor(int constantValue) {
        kind = DescriptorKind.CONSTANT;
        type = new Type(TypeKind.INT);
        value = constantValue;
    }

    public Descriptor(TokenKind conditionOperator) {
        kind = DescriptorKind.CONDITION;
        condition = Condition.fromToken(conditionOperator);

        trueLabel = new Label();
        falseLabel = new Label();
    }

    @Override
    public String toString() {
        return symbol.toString();
    }
}
