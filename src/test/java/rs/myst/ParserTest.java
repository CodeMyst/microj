package rs.myst;

import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    @Test
    void validSampleFiles() throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        final List<URL> sampleFiles = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            sampleFiles.add(classLoader.getResource("Sample" + i + ".mj"));
        }

        for (URL sampleFile : sampleFiles) {
            try (FileReader reader = new FileReader(sampleFile.getPath())) {
                System.out.println("Parsing: " + sampleFile.getFile());

                Scanner scanner = new Scanner(reader);

                Parser parser = new Parser(scanner);

                parser.parse();

                assertTrue(parser.parsedSuccessfully());
            }
        }
    }
}