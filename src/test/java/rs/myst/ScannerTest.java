package rs.myst;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScannerTest {
    @Test
    void validSampleFiles() throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        final List<URL> sampleFiles = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            sampleFiles.add(classLoader.getResource("Sample" + i + ".mj"));
        }

        for (URL sampleFile : sampleFiles) {
            try (FileReader reader = new FileReader(sampleFile.getPath())) {
                Scanner scanner = new Scanner(reader);

                Token token;
                do {
                    token = scanner.next();
                } while (scanner.hasNext());

                assertEquals(TokenKind.EOF, token.getKind());
            }
        }
    }

    @Test
    void invalidSampleFiles() throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        final URL sampleFile = classLoader.getResource("Invalid0.mj");

        assert sampleFile != null;

        try (FileReader reader = new FileReader(sampleFile.getPath())) {
            Scanner scanner = new Scanner(reader);

            Token token;
            do {
                token = scanner.next();
            } while (scanner.hasNext());

            assertEquals(TokenKind.INVALID, token.getKind());
        }
    }
}