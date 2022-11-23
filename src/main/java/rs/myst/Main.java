package rs.myst;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class Main {
    public static void main(String[] args) throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final URL sampleUrl = classLoader.getResource("Sample0.mj");

        if (sampleUrl == null) {
            System.out.println("File not found");
            return;
        }

        try (FileReader reader = new FileReader(sampleUrl.getPath())) {
            final Scanner scanner = new Scanner(reader);

            final Parser parser = new Parser(scanner);

            parser.parse();

            if (!parser.parsedSuccessfully()) {
                System.out.println("File has " + parser.getErrorCount() + " error(s).");
            }

//            Token token;
//            do {
//                token = scanner.next();
//
//                System.out.println(token.toString());
//            } while (scanner.hasNext());
        }
    }
}