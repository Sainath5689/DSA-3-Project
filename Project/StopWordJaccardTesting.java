import java.io.*;
import java.util.*;

public class StopWordJaccardTesting {

    static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "what", "is", "a", "an", "the", "how", "do", "i",
            "can", "to", "of", "in", "on", "for", "and", "or",
            "explain", "give", "me", "please", "with", "from",
            "does", "it", "this", "that", "be", "using"
    ));

    static Set<String> getMeaningfulWords(String text) {

        text = text.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Set<String> words = new HashSet<>();

        if (!text.isEmpty()) {

            for (String word : text.split(" ")) {

                if (!STOP_WORDS.contains(word)) {
                    words.add(word);
                }
            }
        }

        return words;
    }

    static double jaccardSimilarity(String a, String b) {

        Set<String> words1 = getMeaningfulWords(a);
        Set<String> words2 = getMeaningfulWords(b);

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

    public static void main(String[] args) throws Exception {

        double[] thresholds = {
                0.10, 0.20, 0.30, 0.40,
                0.50, 0.60, 0.70, 0.80
        };

        List<Double> similarities = new ArrayList<>();
        List<Integer> actualLabels = new ArrayList<>();

        BufferedReader br = new BufferedReader(
                new FileReader("Near_Duplicate_Question_Dataset_1000.csv"));

        br.readLine();

        String line;

        while ((line = br.readLine()) != null) {

            List<String> parts = parseCSV(line);

            if (parts.size() < 4) {
                continue;
            }

            String question1 = parts.get(1);
            String question2 = parts.get(2);

            int actualLabel = Integer.parseInt(parts.get(3));

            double similarity =
                    jaccardSimilarity(question1, question2);

            similarities.add(similarity);
            actualLabels.add(actualLabel);
        }

        br.close();

        System.out.println(
                "Threshold\tAccuracy\tPrecision\tRecall\t\tF1-score");

        System.out.println(
                "-------------------------------------------------------------");

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
                } else {
                    fn++;
                }
            }

            double accuracy =
                    (double) (tp + tn) / similarities.size();

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