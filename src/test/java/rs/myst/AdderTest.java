package rs.myst;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdderTest {
    @Test
    void add() {
        final Adder adder = new Adder();

        assertEquals(adder.add(35, 34), 69);
    }
}