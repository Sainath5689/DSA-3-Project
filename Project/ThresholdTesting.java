import java.io.*;
import java.util.*;

public class ThresholdTesting {

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

        double[] thresholds = {
                0.10, 0.15, 0.20, 0.25, 0.30,
                0.35, 0.40, 0.45, 0.50, 0.60
        };

        List<Double> similarities = new ArrayList<>();
        List<Integer> actualLabels = new ArrayList<>();

        BufferedReader br = new BufferedReader(
                new FileReader("Near_Duplicate_Question_Dataset_1000.csv"));

        br.readLine();

        String line;

        while ((line = br.readLine()) != null) {

            List<String> parts = parseCSV(line);

            if (parts.size() < 4)
                continue;

            String q1 = clean(parts.get(1));
            String q2 = clean(parts.get(2));

            int actual = Integer.parseInt(parts.get(3));

            int commonLength = longestCommonSubstring(q1, q2);

            int maximumLength = Math.max(q1.length(), q2.length());

            double similarity = 0.0;

            if (maximumLength > 0) {
                similarity = (double) commonLength / maximumLength;
            }

            similarities.add(similarity);
            actualLabels.add(actual);
        }

        br.close();

        System.out.println("Threshold\tAccuracy\tPrecision\tRecall\t\tF1-score");
        System.out.println("-------------------------------------------------------------");

        for (double threshold : thresholds) {

            int tp = 0;
            int tn = 0;
            int fp = 0;
            int fn = 0;

            for (int i = 0; i < similarities.size(); i++) {

                int actual = actualLabels.get(i);

                int predicted =
                        similarities.get(i) >= threshold ? 1 : 0;

                if (actual == 1 && predicted == 1) {
                    tp++;
                } else if (actual == 0 && predicted == 0) {
                    tn++;
                } else if (actual == 0 && predicted == 1) {
                    fp++;
                } else if (actual == 1 && predicted == 0) {
                    fn++;
                }
            }

            double accuracy = (double) (tp + tn)
                    / similarities.size();

            double precision = (tp + fp) == 0
                    ? 0
                    : (double) tp / (tp + fp);

            double recall = (tp + fn) == 0
                    ? 0
                    : (double) tp / (tp + fn);

            double f1 = (precision + recall) == 0
                    ? 0
                    : 2 * precision * recall
                            / (precision + recall);

            System.out.printf(
                    "%.2f\t\t%.2f%%\t\t%.2f%%\t\t%.2f%%\t\t%.2f%%%n",
                    threshold,
                    accuracy * 100,
                    precision * 100,
                    recall * 100,
                    f1 * 100
            );
        }
    }
}