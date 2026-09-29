import java.io.*;
import java.util.*;

public class ExactVerification {

    static int longestCommonSubstring(String a, String b) {

        int[][] dp = new int[a.length() + 1][b.length() + 1];
        int maxLength = 0;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {

                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    maxLength = Math.max(maxLength, dp[i][j]);
                }
            }
        }

        return maxLength;
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

    static String clean(String text) {

        return text.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
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
                "question_id_1,question_1,question_id_2,question_2," +
                "common_length,similarity_score,result"
        );

        String line;
        int total = 0;
        int nearDuplicates = 0;

        while ((line = br.readLine()) != null) {

            List<String> parts = parseCSV(line);

            if (parts.size() < 4)
                continue;

            String id1 = parts.get(0);
            String q1 = clean(parts.get(1));
            String id2 = parts.get(2);
            String q2 = clean(parts.get(3));

            int commonLength = longestCommonSubstring(q1, q2);

            int maximumLength = Math.max(q1.length(), q2.length());

            double similarity = 0.0;

            if (maximumLength > 0) {
                similarity = (double) commonLength / maximumLength;
            }

            String result;

            if (similarity >= 0.10) {
                result = "Near Duplicate";
                nearDuplicates++;
            } else {
                result = "Not Duplicate";
            }

            pw.println(
                    csv(id1) + "," +
                    csv(q1) + "," +
                    csv(id2) + "," +
                    csv(q2) + "," +
                    commonLength + "," +
                    String.format(Locale.US, "%.3f", similarity) + "," +
                    result
            );

            total++;
        }

        br.close();
        pw.close();

        System.out.println("Exact verification completed.");
        System.out.println("Candidate pairs checked: " + total);
        System.out.println("Near duplicate pairs: " + nearDuplicates);
        System.out.println("Output file: final_results.csv");
    }
}