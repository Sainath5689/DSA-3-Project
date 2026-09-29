import java.io.*;
import java.util.*;

public class JaccardExactVerification {

    static Set<String> getWords(String text) {

        text = text.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Set<String> words = new HashSet<>();

        if (!text.isEmpty()) {
            words.addAll(Arrays.asList(text.split(" ")));
        }

        return words;
    }

    static double jaccardSimilarity(String a, String b) {

        Set<String> words1 = getWords(a);
        Set<String> words2 = getWords(b);

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    static List<String> parseCSV(String line) {

        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);

            if (c == '"') {

                if (quoted && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    field.append('"');
                    i++;

                } else {
                    quoted = !quoted;
                }

            } else if (c == ',' && !quoted) {

                fields.add(field.toString());
                field.setLength(0);

            } else {
                field.append(c);
            }
        }

        fields.add(field.toString());

        return fields;
    }

    static String csv(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    public static void main(String[] args) throws Exception {

        BufferedReader br = new BufferedReader(
                new FileReader("candidate_pairs.csv"));

        PrintWriter pw = new PrintWriter(
                new FileWriter("final_results.csv"));

        br.readLine();

        pw.println(
                "question_id_1,question_1,question_id_2," +
                "question_2,jaccard_similarity,result"
        );

        String line;

        int total = 0;
        int nearDuplicates = 0;

        while ((line = br.readLine()) != null) {

            List<String> parts = parseCSV(line);

            if (parts.size() < 4) {
                continue;
            }

            String id1 = parts.get(0);
            String question1 = parts.get(1);

            String id2 = parts.get(2);
            String question2 = parts.get(3);

            double similarity =
                    jaccardSimilarity(question1, question2);

            String result;

            if (similarity >= 0.10) {
                result = "Near Duplicate";
                nearDuplicates++;
            } else {
                result = "Not Duplicate";
            }

            pw.println(
                    csv(id1) + "," +
                    csv(question1) + "," +
                    csv(id2) + "," +
                    csv(question2) + "," +
                    String.format(Locale.US, "%.3f", similarity) +
                    "," +
                    result
            );

            total++;
        }

        br.close();
        pw.close();

        System.out.println("Jaccard exact verification completed.");
        System.out.println("Candidate pairs checked: " + total);
        System.out.println("Near duplicate pairs: " + nearDuplicates);
        System.out.println("Output file: final_results.csv");
    }
}