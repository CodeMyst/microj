package rs.myst;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SymbolTableTest {
    @Test
    void insert() {
        SymbolTable symbolTable = new SymbolTable();

        symbolTable.openScope();

            Symbol iSym = symbolTable.insert(SymbolKind.VARIABLE, "i", new Type(TypeKind.INT));

            assertNotNull(iSym);

            assertEquals(0, iSym.getAddress());
            assertEquals(ScopeType.GLOBAL, iSym.getScopeType());

            Symbol jSym = symbolTable.insert(SymbolKind.VARIABLE, "j", new Type(TypeKind.INT));

            assertNotNull(jSym);

            assertEquals(1, jSym.getAddress());
            assertEquals(ScopeType.GLOBAL, jSym.getScopeType());

            Symbol i2Sym = symbolTable.insert(SymbolKind.VARIABLE, "i", new Type(TypeKind.INT));

            assertNull(i2Sym);

        symbolTable.closeScope();

        symbolTable.openScope();

            Symbol i3Sym = symbolTable.insert(SymbolKind.VARIABLE, "i", new Type(TypeKind.INT));

            assertNotNull(i3Sym);

        symbolTable.closeScope();
    }

    @Test
    void findByName() {
        SymbolTable symbolTable = new SymbolTable();

        symbolTable.openScope();

            Symbol iSym = symbolTable.insert(SymbolKind.VARIABLE, "i", new Type(TypeKind.INT));

            assertNotNull(iSym);

            Symbol i2Sym = symbolTable.findByName("i");

            assertEquals(iSym, i2Sym);

        symbolTable.closeScope();
    }
}