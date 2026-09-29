import java.io.*;
import java.util.*;

public class FinalPipeline {

    // ---------------- SETTINGS ----------------

    static final int K = 3;

    static final int NUM_HASHES = 12;
    static final int BANDS = 4;
    static final int ROWS = 3;

    static final double THRESHOLD = 0.10;

    static final long MOD1 = 1000000007L;
    static final long MOD2 = 1000000009L;

    static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "what", "is", "a", "an", "the",
            "how", "do", "i", "can", "to",
            "of", "in", "on", "for", "and",
            "or", "explain", "give", "me",
            "please", "with", "from", "does",
            "it", "this", "that", "be", "using"
    ));

    // ---------------- QUESTION CLASS ----------------

    static class Question {

        String id;
        String text;
        int originalId;
        int side;

        Question(String id, String text,
                 int originalId, int side) {

            this.id = id;
            this.text = text;
            this.originalId = originalId;
            this.side = side;
        }
    }

    // ---------------- CSV PARSER ----------------

    static List<String> parseCSV(String line) {

        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();

        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);

            if (c == '"') {

                if (quoted &&
                    i + 1 < line.length() &&
                    line.charAt(i + 1) == '"') {

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

        return "\"" +
                text.replace("\"", "\"\"") +
                "\"";
    }

    // ---------------- PREPROCESSING ----------------

    static String clean(String text) {

        return text.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ---------------- SHINGLING ----------------

    static Set<String> getShingles(String text) {

        String[] words = clean(text).split(" ");

        Set<String> shingles = new HashSet<>();

        if (words.length < K) {
            shingles.add(String.join(" ", words));
            return shingles;
        }

        for (int i = 0; i <= words.length - K; i++) {

            StringBuilder shingle = new StringBuilder();

            for (int j = 0; j < K; j++) {

                if (j > 0)
                    shingle.append(" ");

                shingle.append(words[i + j]);
            }

            shingles.add(shingle.toString());
        }

        return shingles;
    }

    // ---------------- DOUBLE POLYNOMIAL HASH ----------------

    static long hash1(String text) {

        long hash = 0;

        for (char c : text.toCharArray()) {

            hash = (hash * 31 + c) % MOD1;
        }

        return hash;
    }

    static long hash2(String text) {

        long hash = 0;

        for (char c : text.toCharArray()) {

            hash = (hash * 37 + c) % MOD2;
        }

        return hash;
    }

    // ---------------- MINHASH SIGNATURE ----------------

    static long[] createSignature(String text) {

        long[] signature =
                new long[NUM_HASHES];

        Arrays.fill(signature, Long.MAX_VALUE);

        Set<String> shingles =
                getShingles(text);

        for (String shingle : shingles) {

            long h1 = hash1(shingle);
            long h2 = hash2(shingle);

            for (int i = 0; i < NUM_HASHES; i++) {

                long value =
                        (h1 * (i + 1)
                        + h2 * (i + 3)
                        + i * 1009L)
                        % 2147483647L;

                if (value < signature[i]) {
                    signature[i] = value;
                }
            }
        }

        return signature;
    }

    // ---------------- LSH ----------------

    static Set<String> generateCandidates(
            List<Question> questions) {

        Set<String> candidates =
                new HashSet<>();

        Map<String, List<Integer>> buckets =
                new HashMap<>();

        long[][] signatures =
                new long[questions.size()][];

        // Create signatures

        for (int i = 0; i < questions.size(); i++) {

            signatures[i] =
                    createSignature(
                            questions.get(i).text);
        }

        // Create LSH buckets

        for (int i = 0; i < questions.size(); i++) {

            long[] signature =
                    signatures[i];

            for (int band = 0; band < BANDS; band++) {

                StringBuilder key =
                        new StringBuilder();

                key.append(band);

                int start = band * ROWS;

                for (int r = 0; r < ROWS; r++) {

                    key.append("_")
                       .append(signature[start + r]);
                }

                String bucketKey =
                        key.toString();

                buckets
                        .computeIfAbsent(
                                bucketKey,
                                k -> new ArrayList<>())
                        .add(i);
            }
        }

        // Generate candidate pairs

        for (List<Integer> bucket :
                buckets.values()) {

            for (int i = 0;
                 i < bucket.size();
                 i++) {

                for (int j = i + 1;
                     j < bucket.size();
                     j++) {

                    int a = bucket.get(i);
                    int b = bucket.get(j);

                    String id1 =
                            questions.get(a).id;

                    String id2 =
                            questions.get(b).id;

                    String key;

                    if (id1.compareTo(id2) < 0) {

                        key = id1 + "|" + id2;

                    } else {

                        key = id2 + "|" + id1;
                    }

                    candidates.add(key);
                }
            }
        }

        return candidates;
    }

    // ---------------- STOP WORDS ----------------

    static Set<String> getMeaningfulWords(
            String text) {

        String cleaned =
                clean(text);

        Set<String> words =
                new HashSet<>();

        if (cleaned.isEmpty())
            return words;

        for (String word :
                cleaned.split(" ")) {

            if (!STOP_WORDS.contains(word)) {

                words.add(word);
            }
        }

        return words;
    }

    // ---------------- JACCARD ----------------

    static double jaccard(
            String text1,
            String text2) {

        Set<String> a =
                getMeaningfulWords(text1);

        Set<String> b =
                getMeaningfulWords(text2);

        Set<String> intersection =
                new HashSet<>(a);

        intersection.retainAll(b);

        Set<String> union =
                new HashSet<>(a);

        union.addAll(b);

        if (union.isEmpty())
            return 0.0;

        return (double) intersection.size()
                / union.size();
    }

    // ---------------- MAIN ----------------

    public static void main(String[] args)
            throws Exception {

        String inputFile =
                "Near_Duplicate_Question_Dataset_1000.csv";

        String candidateFile =
                "corrected_candidate_pairs.csv";

        String resultFile =
                "final_pipeline_results.csv";

        BufferedReader br =
                new BufferedReader(
                        new FileReader(inputFile));

        br.readLine();

        List<Question> questions =
                new ArrayList<>();

        List<String[]> originalPairs =
                new ArrayList<>();

        List<Integer> actualLabels =
                new ArrayList<>();

        String line;

        // Read original dataset

        while ((line = br.readLine()) != null) {

            List<String> parts =
                    parseCSV(line);

            if (parts.size() < 4)
                continue;

            int id =
                    Integer.parseInt(parts.get(0));

            String q1 =
                    parts.get(1);

            String q2 =
                    parts.get(2);

            int label =
                    Integer.parseInt(parts.get(3));

            questions.add(
                    new Question(
                            id + "A",
                            q1,
                            id,
                            1));

            questions.add(
                    new Question(
                            id + "B",
                            q2,
                            id,
                            2));

            originalPairs.add(
                    new String[]{q1, q2});

            actualLabels.add(label);
        }

        br.close();

        System.out.println(
                "Original pairs: " +
                originalPairs.size());

        System.out.println(
                "Total questions: " +
                questions.size());

        // ---------------- LSH ----------------

        System.out.println();
        System.out.println(
                "Generating LSH candidates...");

        Set<String> candidates =
                generateCandidates(
                        questions);

        System.out.println(
                "LSH candidate pairs: " +
                candidates.size());

        // ---------------- SAVE CANDIDATES ----------------

        PrintWriter candidateWriter =
                new PrintWriter(
                        new FileWriter(candidateFile));

        candidateWriter.println(
                "question_id_1,question_1," +
                "question_id_2,question_2");

        for (String key : candidates) {

            String[] ids =
                    key.split("\\|");

            Question q1 = null;
            Question q2 = null;

            for (Question q :
                    questions) {

                if (q.id.equals(ids[0]))
                    q1 = q;

                if (q.id.equals(ids[1]))
                    q2 = q;
            }

            if (q1 != null && q2 != null) {

                candidateWriter.println(
                        csv(q1.id) + "," +
                        csv(q1.text) + "," +
                        csv(q2.id) + "," +
                        csv(q2.text));
            }
        }

        candidateWriter.close();

        // ---------------- EVALUATION ----------------

        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;

        int lshFound = 0;

        PrintWriter resultWriter =
                new PrintWriter(
                        new FileWriter(resultFile));

        resultWriter.println(
                "id,question_1,question_2," +
                "actual_label,jaccard_similarity," +
                "lsh_candidate,predicted_label,result");

        for (int i = 0;
             i < originalPairs.size();
             i++) {

            int originalId = i + 1;

            String q1 =
                    originalPairs.get(i)[0];

            String q2 =
                    originalPairs.get(i)[1];

            int actual =
                    actualLabels.get(i);

            String idA =
                    originalId + "A";

            String idB =
                    originalId + "B";

            String pairKey =
                    idA + "|" + idB;

            boolean candidate =
                    candidates.contains(pairKey);

            double similarity =
                    jaccard(q1, q2);

            int predicted = 0;

            if (candidate) {

                lshFound++;

                if (similarity >= THRESHOLD) {
                    predicted = 1;
                }
            }

            String result =
                    predicted == 1
                    ? "Near Duplicate"
                    : "Not Duplicate";

            if (actual == 1 &&
                predicted == 1) {

                tp++;

            } else if (actual == 0 &&
                       predicted == 0) {

                tn++;

            } else if (actual == 0 &&
                       predicted == 1) {

                fp++;

            } else {

                fn++;
            }

            resultWriter.println(
                    originalId + "," +
                    csv(q1) + "," +
                    csv(q2) + "," +
                    actual + "," +
                    String.format(
                            Locale.US,
                            "%.3f",
                            similarity) + "," +
                    candidate + "," +
                    predicted + "," +
                    result);
        }

        resultWriter.close();

        // ---------------- METRICS ----------------

        int total =
                originalPairs.size();

        double accuracy =
                (double)(tp + tn) / total;

        double precision =
                (tp + fp) == 0
                ? 0
                : (double)tp / (tp + fp);

        double recall =
                (tp + fn) == 0
                ? 0
                : (double)tp / (tp + fn);

        double f1 =
                (precision + recall) == 0
                ? 0
                : 2 * precision * recall
                / (precision + recall);

        System.out.println();
        System.out.println(
                "===== FINAL PIPELINE RESULTS =====");

        System.out.println(
                "Total original pairs: " +
                total);

        System.out.println(
                "LSH candidates generated: " +
                candidates.size());

        System.out.println(
                "Original pairs found by LSH: " +
                lshFound);

        System.out.println();

        System.out.println(
                "True Positives: " + tp);

        System.out.println(
                "True Negatives: " + tn);

        System.out.println(
                "False Positives: " + fp);

        System.out.println(
                "False Negatives: " + fn);

        System.out.println();

        System.out.printf(
                "Accuracy: %.2f%%%n",
                accuracy * 100);

        System.out.printf(
                "Precision: %.2f%%%n",
                precision * 100);

        System.out.printf(
                "Recall: %.2f%%%n",
                recall * 100);

        System.out.printf(
                "F1-score: %.2f%%%n",
                f1 * 100);

        System.out.println();

        System.out.println(
                "Candidate file: " +
                candidateFile);

        System.out.println(
                "Final result file: " +
                resultFile);
    }
}