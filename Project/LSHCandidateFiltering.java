import java.io.*;
import java.util.*;

public class LSHCandidateFiltering {

    static final int BANDS = 4;
    static final int ROWS = 3;

    static class Question {
        String id;
        String text;
        String hashes;

        Question(String id, String text, String hashes) {
            this.id = id;
            this.text = text;
            this.hashes = hashes;
        }
    }

    public static void main(String[] args) throws Exception {

        BufferedReader questionsReader = new BufferedReader(
                new FileReader("cleaned_questions.csv"));

        BufferedReader hashesReader = new BufferedReader(
                new FileReader("hashed_shingles.csv"));

        questionsReader.readLine();
        hashesReader.readLine();

        List<Question> questions = new ArrayList<>();

        String questionLine;
        String hashLine;

        while ((questionLine = questionsReader.readLine()) != null
                && (hashLine = hashesReader.readLine()) != null) {

            String[] qParts = questionLine.split(",", 3);
            String[] hParts = hashLine.split(",", 3);

            if (qParts.length < 3 || hParts.length < 3)
                continue;

            String id = qParts[0];
            String questionText = qParts[1];

            String hashValues = hParts[1] + "|" + hParts[2];

            questions.add(
                    new Question(id, questionText, hashValues)
            );
        }

        questionsReader.close();
        hashesReader.close();

        Map<String, List<Integer>> buckets = new HashMap<>();

        for (int i = 0; i < questions.size(); i++) {

            String[] hashes = questions.get(i).hashes.split("\\|");

            for (int band = 0; band < BANDS; band++) {

                StringBuilder key = new StringBuilder();
                key.append(band).append(":");

                int start = band * ROWS;

                for (int j = 0; j < ROWS; j++) {

                    int position = start + j;

                    if (position < hashes.length) {
                        key.append(hashes[position]).append("-");
                    }
                }

                buckets
                        .computeIfAbsent(key.toString(),
                                k -> new ArrayList<>())
                        .add(i);
            }
        }

        Set<String> candidatePairs = new HashSet<>();

        for (List<Integer> bucket : buckets.values()) {

            for (int i = 0; i < bucket.size(); i++) {

                for (int j = i + 1; j < bucket.size(); j++) {

                    int first = bucket.get(i);
                    int second = bucket.get(j);

                    String pair = Math.min(first, second)
                            + ":"
                            + Math.max(first, second);

                    candidatePairs.add(pair);
                }
            }
        }

        PrintWriter writer = new PrintWriter(
                new FileWriter("candidate_pairs.csv"));

        writer.println(
                "question_id_1,question_1,question_id_2,question_2"
        );

        for (String pair : candidatePairs) {

            String[] indexes = pair.split(":");

            int first = Integer.parseInt(indexes[0]);
            int second = Integer.parseInt(indexes[1]);

            Question q1 = questions.get(first);
            Question q2 = questions.get(second);

            writer.println(
                    csv(q1.id) + "," +
                    csv(q1.text) + "," +
                    csv(q2.id) + "," +
                    csv(q2.text)
            );
        }

        writer.close();

        System.out.println("Corrected LSH filtering completed.");
        System.out.println("Questions processed: " + questions.size());
        System.out.println("LSH buckets created: " + buckets.size());
        System.out.println("Candidate pairs generated: " + candidatePairs.size());
        System.out.println("Output file: candidate_pairs.csv");
    }

    static String csv(String text) {

        text = text.replace("\"", "\"\"");

        return "\"" + text + "\"";
    }
}