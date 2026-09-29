import java.io.*;
import java.util.*;

public class EvaluateOriginalPairs {

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

    public static void main(String[] args) throws Exception {

        BufferedReader br = new BufferedReader(
                new FileReader("Near_Duplicate_Question_Dataset_1000.csv"));

        br.readLine();

        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;

        int total = 0;

        String line;

        while ((line = br.readLine()) != null) {

            List<String> parts = parseCSV(line);

            if (parts.size() < 4)
                continue;

            String question1 = clean(parts.get(1));
            String question2 = clean(parts.get(2));

            int actualLabel = Integer.parseInt(parts.get(3));

            int commonLength = longestCommonSubstring(question1, question2);

            int maximumLength = Math.max(question1.length(), question2.length());

            double similarity = 0.0;

            if (maximumLength > 0) {
                similarity = (double) commonLength / maximumLength;
            }

            int predictedLabel = similarity >= 0.60 ? 1 : 0;

            if (actualLabel == 1 && predictedLabel == 1) {
                tp++;
            } else if (actualLabel == 0 && predictedLabel == 0) {
                tn++;
            } else if (actualLabel == 0 && predictedLabel == 1) {
                fp++;
            } else if (actualLabel == 1 && predictedLabel == 0) {
                fn++;
            }

            total++;
        }

        br.close();

        double accuracy = (double) (tp + tn) / total;

        double precision = (tp + fp) == 0
                ? 0
                : (double) tp / (tp + fp);

        double recall = (tp + fn) == 0
                ? 0
                : (double) tp / (tp + fn);

        double f1 = (precision + recall) == 0
                ? 0
                : 2 * precision * recall / (precision + recall);

        System.out.println("Evaluation completed.");
        System.out.println("--------------------");
        System.out.println("Total original pairs: " + total);
        System.out.println("True Positives: " + tp);
        System.out.println("True Negatives: " + tn);
        System.out.println("False Positives: " + fp);
        System.out.println("False Negatives: " + fn);
        System.out.printf("Accuracy: %.2f%%%n", accuracy * 100);
        System.out.printf("Precision: %.2f%%%n", precision * 100);
        System.out.printf("Recall: %.2f%%%n", recall * 100);
        System.out.printf("F1-score: %.2f%%%n", f1 * 100);
    }
}