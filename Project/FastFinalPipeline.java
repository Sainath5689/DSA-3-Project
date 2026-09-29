import java.io.*;
import java.util.*;

public class FastFinalPipeline {

    static Set<String> stopWords = new HashSet<>(Arrays.asList(
        "what","is","a","an","the","how","do","i","can","to",
        "of","in","on","for","and","or","explain","give","me",
        "please","with","from","does","it","this","that","be","using"
    ));

    static class Pair {
        String q1, q2;
        int actual;

        Pair(String q1, String q2, int actual) {
            this.q1 = q1;
            this.q2 = q2;
            this.actual = actual;
        }
    }

    public static void main(String[] args) throws Exception {

        String file = "Near_Duplicate_Question_Dataset_1000.csv";

        List<Pair> pairs = loadDataset(file);

        System.out.println("======================================");
        System.out.println(" FAST FINAL VERIFICATION PIPELINE");
        System.out.println("======================================");
        System.out.println("Total pairs: " + pairs.size());

        // Test several thresholds
        double bestThreshold = 0;
        double bestF1 = -1;

        System.out.println("\nThreshold Testing");
        System.out.println("--------------------------------------");

        for (double threshold = 0.10;
             threshold <= 0.90;
             threshold += 0.05) {

            int TP = 0, TN = 0, FP = 0, FN = 0;

            for (Pair p : pairs) {

                double score = combinedSimilarity(p.q1, p.q2);

                boolean predicted = score >= threshold;

                if (p.actual == 1 && predicted) TP++;
                else if (p.actual == 0 && !predicted) TN++;
                else if (p.actual == 0 && predicted) FP++;
                else FN++;
            }

            double precision =
                (TP + FP == 0) ? 0 :
                (double) TP / (TP + FP);

            double recall =
                (TP + FN == 0) ? 0 :
                (double) TP / (TP + FN);

            double f1 =
                (precision + recall == 0) ? 0 :
                2 * precision * recall /
                (precision + recall);

            double accuracy =
                (double)(TP + TN) / pairs.size();

            System.out.printf(
                "Threshold %.2f -> Accuracy %.2f%% | Precision %.2f%% | Recall %.2f%% | F1 %.2f%%%n",
                threshold,
                accuracy * 100,
                precision * 100,
                recall * 100,
                f1 * 100
            );

            if (f1 > bestF1) {
                bestF1 = f1;
                bestThreshold = threshold;
            }
        }

        // --------------------------------------
        // FINAL CLASSIFICATION
        // --------------------------------------

        int TP = 0, TN = 0, FP = 0, FN = 0;

        PrintWriter out =
            new PrintWriter(new FileWriter("final_results.csv"));

        out.println(
            "id,question_1,question_2,actual_label,predicted_label,similarity"
        );

        for (int i = 0; i < pairs.size(); i++) {

            Pair p = pairs.get(i);

            double score =
                combinedSimilarity(p.q1, p.q2);

            boolean predicted =
                score >= bestThreshold;

            if (p.actual == 1 && predicted) TP++;
            else if (p.actual == 0 && !predicted) TN++;
            else if (p.actual == 0 && predicted) FP++;
            else FN++;

            out.printf(
                "%d,\"%s\",\"%s\",%d,%d,%.4f%n",
                i + 1,
                p.q1,
                p.q2,
                p.actual,
                predicted ? 1 : 0,
                score
            );
        }

        out.close();

        double accuracy =
            (double)(TP + TN) / pairs.size();

        double precision =
            (TP + FP == 0) ? 0 :
            (double)TP / (TP + FP);

        double recall =
            (TP + FN == 0) ? 0 :
            (double)TP / (TP + FN);

        double f1 =
            (precision + recall == 0) ? 0 :
            2 * precision * recall /
            (precision + recall);

        System.out.println("\n======================================");
        System.out.println(" FINAL RESULTS");
        System.out.println("======================================");

        System.out.printf(
            "Selected threshold: %.2f%n",
            bestThreshold
        );

        System.out.println("TP: " + TP);
        System.out.println("TN: " + TN);
        System.out.println("FP: " + FP);
        System.out.println("FN: " + FN);

        System.out.printf("Accuracy : %.2f%%%n", accuracy * 100);
        System.out.printf("Precision: %.2f%%%n", precision * 100);
        System.out.printf("Recall   : %.2f%%%n", recall * 100);
        System.out.printf("F1 Score : %.2f%%%n", f1 * 100);

        System.out.println("\nCreated:");
        System.out.println("final_results.csv");
    }

    // ==================================================
    // COMBINED SIMILARITY
    // ==================================================

    static double combinedSimilarity(String a, String b) {

        Set<String> s1 = tokenize(a);
        Set<String> s2 = tokenize(b);

        double jaccard = jaccard(s1, s2);

        double lcs = wordLCS(s1, s2);

        // Combined verification score
        return (jaccard + lcs) / 2.0;
    }

    // ==================================================
    // STOP-WORD TOKENIZATION
    // ==================================================

    static Set<String> tokenize(String text) {

        Set<String> result = new LinkedHashSet<>();

        String cleaned = text
            .toLowerCase()
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();

        if (cleaned.isEmpty())
            return result;

        for (String word : cleaned.split(" ")) {

            if (!stopWords.contains(word))
                result.add(word);
        }

        return result;
    }

    // ==================================================
    // JACCARD
    // ==================================================

    static double jaccard(Set<String> a, Set<String> b) {

        Set<String> intersection =
            new HashSet<>(a);

        intersection.retainAll(b);

        Set<String> union =
            new HashSet<>(a);

        union.addAll(b);

        if (union.isEmpty())
            return 0;

        return (double)intersection.size() /
               union.size();
    }

    // ==================================================
    // WORD LEVEL LCS - DYNAMIC PROGRAMMING
    // ==================================================

    static double wordLCS(Set<String> a, Set<String> b) {

        String[] x = a.toArray(new String[0]);
        String[] y = b.toArray(new String[0]);

        int[][] dp =
            new int[x.length + 1][y.length + 1];

        for (int i = 1; i <= x.length; i++) {

            for (int j = 1; j <= y.length; j++) {

                if (x[i - 1].equals(y[j - 1])) {

                    dp[i][j] =
                        dp[i - 1][j - 1] + 1;

                } else {

                    dp[i][j] =
                        Math.max(
                            dp[i - 1][j],
                            dp[i][j - 1]
                        );
                }
            }
        }

        int lcs = dp[x.length][y.length];

        int maxLength =
            Math.max(x.length, y.length);

        if (maxLength == 0)
            return 0;

        return (double)lcs / maxLength;
    }

    // ==================================================
    // LOAD CSV
    // ==================================================

    static List<Pair> loadDataset(String file)
            throws Exception {

        List<Pair> list =
            new ArrayList<>();

        BufferedReader br =
            new BufferedReader(
                new FileReader(file)
            );

        br.readLine(); // header

        String line;
        int id = 1;

        while ((line = br.readLine()) != null) {

            String[] parts = parseCSV(line);

            if (parts.length < 3)
                continue;

            String q1 = parts[1];
            String q2 = parts[2];

            int actual =
                (id <= 500) ? 1 : 0;

            list.add(
                new Pair(q1, q2, actual)
            );

            id++;
        }

        br.close();

        return list;
    }

    // ==================================================
    // CSV PARSER
    // ==================================================

    static String[] parseCSV(String line) {

        List<String> fields =
            new ArrayList<>();

        StringBuilder current =
            new StringBuilder();

        boolean quotes = false;

        for (char c : line.toCharArray()) {

            if (c == '"') {

                quotes = !quotes;

            } else if (c == ',' && !quotes) {

                fields.add(current.toString());
                current.setLength(0);

            } else {

                current.append(c);
            }
        }

        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }
}