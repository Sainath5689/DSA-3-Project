import java.io.*;
import java.util.*;

public class CorrectedFinalPipeline {

    static final int K = 3;
    static final int NUM_HASHES = 24;
    static final int BANDS = 8;
    static final int ROWS = 3;
    static final long PRIME = 2147483647L;

    static class Question {
        String id;
        String text;
        Set<String> shingles;

        Question(String id, String text) {
            this.id = id;
            this.text = text;
            this.shingles = makeShingles(text);
        }
    }

    static List<Question> questions = new ArrayList<>();
    static Map<String, Question> questionMap = new HashMap<>();

    // Stop words used during final verification
    static Set<String> stopWords = new HashSet<>(Arrays.asList(
        "what", "is", "a", "an", "the", "how", "do", "i",
        "can", "to", "of", "in", "on", "for", "and", "or",
        "explain", "give", "me", "please", "with", "from",
        "does", "it", "this", "that", "be", "using"
    ));

    static long[] A = new long[NUM_HASHES];
    static long[] B = new long[NUM_HASHES];

    public static void main(String[] args) throws Exception {

        initializeHashFunctions();

        String input = "Near_Duplicate_Question_Dataset_1000.csv";

        loadDataset(input);

        System.out.println("==========================================");
        System.out.println(" CORRECTED NEAR-DUPLICATE PIPELINE");
        System.out.println("==========================================");
        System.out.println("Original pairs: " + (questions.size() / 2));
        System.out.println("Total questions: " + questions.size());

        // ------------------------------------------
        // STEP 1: LSH candidate generation
        // ------------------------------------------

        Set<String> candidates = generateLSHCandidates();

        System.out.println("\nLSH candidates generated: " + candidates.size());

        writeCandidates(candidates, "corrected_candidate_pairs.csv");

        // ------------------------------------------
        // STEP 2: Check original-pair recovery
        // ------------------------------------------

        int recovered = 0;
        int totalOriginalPairs = questions.size() / 2;

        for (int i = 1; i <= totalOriginalPairs; i++) {

            String id1 = i + "A";
            String id2 = i + "B";

            String key = makeKey(id1, id2);

            if (candidates.contains(key)) {
                recovered++;
            }
        }

        double recall = (recovered * 100.0) / totalOriginalPairs;

        System.out.println("\n========== LSH RECALL CHECK ==========");
        System.out.println("Original pairs: " + totalOriginalPairs);
        System.out.println("Recovered by LSH: " + recovered);
        System.out.printf("LSH pair recall: %.2f%%%n", recall);

        // ------------------------------------------
        // STEP 3: Final verification
        // ------------------------------------------

        int TP = 0;
        int TN = 0;
        int FP = 0;
        int FN = 0;

        PrintWriter resultWriter =
                new PrintWriter(new FileWriter("final_results.csv"));

        resultWriter.println(
            "id,question_1,question_2,actual_label,predicted_label,similarity"
        );

        for (int i = 1; i <= totalOriginalPairs; i++) {

            Question q1 = questionMap.get(i + "A");
            Question q2 = questionMap.get(i + "B");

            String key = makeKey(q1.id, q2.id);

            boolean predicted = false;
            double similarity = 0.0;

            // Only verify pairs that survived LSH
            if (candidates.contains(key)) {

                similarity = jaccard(q1.text, q2.text);

                // Threshold = 0.10
                if (similarity >= 0.10) {
                    predicted = true;
                }
            }

            int actual = 0;

            // Original dataset:
            // first 500 = near duplicate
            // next 500 = not duplicate
            if (i <= 500) {
                actual = 1;
            }

            if (actual == 1 && predicted)
                TP++;
            else if (actual == 0 && !predicted)
                TN++;
            else if (actual == 0 && predicted)
                FP++;
            else
                FN++;

            resultWriter.printf(
                "%d,\"%s\",\"%s\",%d,%d,%.4f%n",
                i,
                q1.text,
                q2.text,
                actual,
                predicted ? 1 : 0,
                similarity
            );
        }

        resultWriter.close();

        // ------------------------------------------
        // STEP 4: Metrics
        // ------------------------------------------

        double accuracy =
                (TP + TN) * 100.0 / totalOriginalPairs;

        double precision =
                (TP + FP == 0) ? 0 :
                TP * 100.0 / (TP + FP);

        double recallMetric =
                (TP + FN == 0) ? 0 :
                TP * 100.0 / (TP + FN);

        double f1 =
                (precision + recallMetric == 0) ? 0 :
                2 * precision * recallMetric /
                (precision + recallMetric);

        System.out.println("\n========== FINAL RESULTS ==========");
        System.out.println("TP: " + TP);
        System.out.println("TN: " + TN);
        System.out.println("FP: " + FP);
        System.out.println("FN: " + FN);

        System.out.printf("Accuracy : %.2f%%%n", accuracy);
        System.out.printf("Precision: %.2f%%%n", precision);
        System.out.printf("Recall   : %.2f%%%n", recallMetric);
        System.out.printf("F1 Score : %.2f%%%n", f1);

        System.out.println("\nFiles created:");
        System.out.println("1. corrected_candidate_pairs.csv");
        System.out.println("2. final_results.csv");
    }

    // ==================================================
    // INITIALIZE PROPER MINHASH FUNCTIONS
    // ==================================================

    static void initializeHashFunctions() {

        long seed = 123456789L;

        for (int i = 0; i < NUM_HASHES; i++) {

            seed = seed * 6364136223846793005L
                    + 1442695040888963407L;

            A[i] = Math.floorMod(seed, PRIME - 1) + 1;

            seed = seed * 6364136223846793005L
                    + 1442695040888963407L;

            B[i] = Math.floorMod(seed, PRIME);
        }
    }

    // ==================================================
    // LOAD DATASET
    // ==================================================

    static void loadDataset(String file) throws Exception {

        BufferedReader br = new BufferedReader(
                new FileReader(file));

        String line;

        // Skip header
        br.readLine();

        int id = 1;

        while ((line = br.readLine()) != null) {

            String[] parts = parseCSV(line);

            if (parts.length < 2)
                continue;

            String q1 = parts[1];
            String q2 = parts[2];

            Question a =
                    new Question(id + "A", q1);

            Question b =
                    new Question(id + "B", q2);

            questions.add(a);
            questions.add(b);

            questionMap.put(a.id, a);
            questionMap.put(b.id, b);

            id++;
        }

        br.close();
    }

    // ==================================================
    // CREATE WORD SHINGLES
    // ==================================================

    static Set<String> makeShingles(String text) {

        Set<String> result = new HashSet<>();

        String cleaned = text
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] words = cleaned.split(" ");

        if (words.length < K) {
            result.add(cleaned);
            return result;
        }

        for (int i = 0; i <= words.length - K; i++) {

            StringBuilder s = new StringBuilder();

            for (int j = 0; j < K; j++) {

                if (j > 0)
                    s.append(" ");

                s.append(words[i + j]);
            }

            result.add(s.toString());
        }

        return result;
    }

    // ==================================================
    // DOUBLE POLYNOMIAL BASE HASH
    // ==================================================

    static long baseHash(String shingle) {

        long h1 = 0;
        long h2 = 0;

        long p1 = 31;
        long p2 = 37;

        for (char c : shingle.toCharArray()) {

            h1 = (h1 * p1 + c) % PRIME;
            h2 = (h2 * p2 + c) % PRIME;
        }

        return (h1 + 31 * h2) % PRIME;
    }

    // ==================================================
    // PROPER MINHASH SIGNATURE
    // ==================================================

    static long[] minHash(Set<String> shingles) {

        long[] signature =
                new long[NUM_HASHES];

        Arrays.fill(signature, Long.MAX_VALUE);

        for (String shingle : shingles) {

            long x = baseHash(shingle);

            for (int i = 0; i < NUM_HASHES; i++) {

                long hash =
                        (A[i] * x + B[i]) % PRIME;

                if (hash < signature[i]) {
                    signature[i] = hash;
                }
            }
        }

        return signature;
    }

    // ==================================================
    // LSH
    // ==================================================

    static Set<String> generateLSHCandidates() {

        Map<String, List<String>> buckets =
                new HashMap<>();

        Map<String, long[]> signatures =
                new HashMap<>();

        // Generate MinHash signatures
        for (Question q : questions) {
            signatures.put(
                    q.id,
                    minHash(q.shingles)
            );
        }

        // Banding
        for (Question q : questions) {

            long[] sig = signatures.get(q.id);

            for (int band = 0; band < BANDS; band++) {

                StringBuilder key =
                        new StringBuilder();

                key.append(band).append(":");

                int start = band * ROWS;

                for (int r = 0; r < ROWS; r++) {
                    key.append(sig[start + r]).append(",");
                }

                String bucketKey = key.toString();

                buckets
                    .computeIfAbsent(
                        bucketKey,
                        k -> new ArrayList<>()
                    )
                    .add(q.id);
            }
        }

        // Generate candidate pairs
        Set<String> candidates =
                new HashSet<>();

        for (List<String> list : buckets.values()) {

            for (int i = 0; i < list.size(); i++) {

                for (int j = i + 1;
                     j < list.size();
                     j++) {

                    String id1 = list.get(i);
                    String id2 = list.get(j);

                    candidates.add(
                            makeKey(id1, id2)
                    );
                }
            }
        }

        return candidates;
    }

    // ==================================================
    // STOP-WORD FILTERED JACCARD
    // ==================================================

    static double jaccard(String a, String b) {

        Set<String> s1 = tokenize(a);
        Set<String> s2 = tokenize(b);

        Set<String> intersection =
                new HashSet<>(s1);

        intersection.retainAll(s2);

        Set<String> union =
                new HashSet<>(s1);

        union.addAll(s2);

        if (union.isEmpty())
            return 0.0;

        return intersection.size() * 1.0 /
                union.size();
    }

    static Set<String> tokenize(String text) {

        Set<String> result =
                new HashSet<>();

        String cleaned = text
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isEmpty())
            return result;

        for (String word : cleaned.split(" ")) {

            if (!stopWords.contains(word)) {
                result.add(word);
            }
        }

        return result;
    }

    // ==================================================
    // KEY
    // ==================================================

    static String makeKey(String a, String b) {

        if (a.compareTo(b) < 0)
            return a + "|" + b;

        return b + "|" + a;
    }

    // ==================================================
    // WRITE CANDIDATE CSV
    // ==================================================

    static void writeCandidates(
            Set<String> candidates,
            String filename) throws Exception {

        PrintWriter out =
                new PrintWriter(new FileWriter(filename));

        out.println(
            "question_id_1,question_1,question_id_2,question_2"
        );

        for (String key : candidates) {

            String[] ids = key.split("\\|");

            Question q1 =
                    questionMap.get(ids[0]);

            Question q2 =
                    questionMap.get(ids[1]);

            if (q1 != null && q2 != null) {

                out.printf(
                    "%s,\"%s\",%s,\"%s\"%n",
                    q1.id,
                    q1.text,
                    q2.id,
                    q2.text
                );
            }
        }

        out.close();
    }

    // ==================================================
    // SIMPLE CSV PARSER
    // ==================================================

    static String[] parseCSV(String line) {

        List<String> fields =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes;
            }
            else if (c == ',' && !insideQuotes) {

                fields.add(current.toString());
                current.setLength(0);

            }
            else {
                current.append(c);
            }
        }

        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }
}