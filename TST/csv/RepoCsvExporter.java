import java.util.List;

/** Writes the repository list as CSV. */
public final class RepoCsvExporter {

    public record Repo(String name, String url, int commits) { }

    public static String export(List<Repo> repos) {
        StringBuilder out = new StringBuilder();
        for (Repo repo : repos) {
            out.append(repo.name()).append(',')
               .append(repo.url()).append(',')
               .append(repo.commits()).append('\n');
        }
        return out.toString();
    }
}
