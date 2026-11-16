import java.util.List;

/** Run with {@code javac *.java && java RepoCsvExporterTest}; fails on the first mismatch. */
public final class RepoCsvExporterTest {

    public static void main(String[] args) {
        check("plain", "tools", RepoCsvExporter.field("tools"));
        check("comma", "\"tools, legacy\"", RepoCsvExporter.field("tools, legacy"));
        check("quote", "\"say \"\"hi\"\"\"", RepoCsvExporter.field("say \"hi\""));
        check("line break", "\"a\nb\"", RepoCsvExporter.field("a\nb"));
        check("whole file",
                "name,url,commits\r\n\"tools, legacy\",https://example.com/t.git,3\r\n",
                RepoCsvExporter.export(List.of(new RepoCsvExporter.Repo("tools, legacy", "https://example.com/t.git", 3))));
        System.out.println("all passed");
    }

    private static void check(String name, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(name + ": expected [" + expected + "] but was [" + actual + "]");
        }
    }
}
