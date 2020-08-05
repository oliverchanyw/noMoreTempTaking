import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Dataformat {

    static final String INPUT = "id.txt";
    static final String OUTPUT = "idjson.txt";

    public static void main(String[] args) {
        List<String> jsons = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT));
             FileWriter writer = new FileWriter(OUTPUT);
        ) {
            String line = reader.readLine();
            while (line != null) {
                String[] tokens = line.split(",");
                String id = tokens[0];
                String name = tokens[1];

                jsons.add(jsonline(id, name));
                line = reader.readLine();
            }

            writer.write("[" + String.join(",\n", jsons) + "]");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String jsonline(String id, String name) {
        return String.format("{\n\t \"id\": \"%s\",\n\t\"name\": \"%s\"\n}", id, name);
    }
}