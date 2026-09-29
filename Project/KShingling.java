
import java.io.*;
import java.util.*;

public class KShingling {

    static ArrayList<String> createShingles(String question, int k) {

        String[] words = question.split("\\s+");
        ArrayList<String> shingles = new ArrayList<>();

        for (int i = 0; i <= words.length - k; i++) {

            StringBuilder shingle = new StringBuilder();

            for (int j = i; j < i + k; j++) {
                if (j > i)
                    shingle.append(" ");

                shingle.append(words[j]);
            }

            shingles.add(shingle.toString());
        }

        return shingles;
    }

    public static void main(String[] args) throws IOException {

        String input = "cleaned_questions.csv";
        String output = "shingles.csv";

        BufferedReader br = new BufferedReader(
                new FileReader(input));

        BufferedWriter bw = new BufferedWriter(
                new FileWriter(output));

        String line;
        int count = 0;
        int k = 3;

        // Read header
        line = br.readLine();

        bw.write("id,question_1_shingles,question_2_shingles");
        bw.newLine();

        while ((line = br.readLine()) != null) {

            String[] parts = line.split(",", 5);

            if (parts.length < 5)
                continue;

            String id = parts[0];
            String q1 = parts[1];
            String q2 = parts[2];

            ArrayList<String> s1 = createShingles(q1, k);
            ArrayList<String> s2 = createShingles(q2, k);

            bw.write(id + ",\"" + String.join(" | ", s1)
                    + "\",\"" + String.join(" | ", s2) + "\"");
            bw.newLine();

            count++;
        }

        br.close();
        bw.close();

        System.out.println("K-Shingling completed!");
        System.out.println("Rows processed: " + count);
        System.out.println("Shingle size (k): " + k);
        System.out.println("Output file: " + output);
    }
}