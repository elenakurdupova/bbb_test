import java.util.List;

/** Writes the repository list as CSV. */
public final class RepoCsvExporter {

    public record Repo(String name, String url, int commits) { }

    public static String export(List<Repo> repos) {
        StringBuilder out = new StringBuilder("name,url,commits\r\n");
        for (Repo repo : repos) {
            out.append(field(repo.name())).append(',')
               .append(field(repo.url())).append(',')
               .append(repo.commits()).append("\r\n");
        }
        return out.toString();
    }

    // RFC 4180: a field with a comma, quote or line break is quoted, and quotes inside it are doubled.
    static String field(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
