import java.io.*;
import java.util.*;

public class Preprocess {

    // Clean one question
    static String clean(String text) {
        return text.toLowerCase()
                   .replaceAll("[^a-z0-9 ]", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    public static void main(String[] args) throws IOException {

        String input = "Near_Duplicate_Question_Dataset_1000.csv";
        String output = "cleaned_questions.csv";

        BufferedReader br = new BufferedReader(
                new FileReader(input));

        BufferedWriter bw = new BufferedWriter(
                new FileWriter(output));

        String line;
        int count = 0;

        // Read header
        line = br.readLine();
        bw.write(line + ",cleaned_question_1,cleaned_question_2");
        bw.newLine();

        // Read every data row
        while ((line = br.readLine()) != null) {

            String[] parts = line.split(",", 5);

            if (parts.length < 5)
                continue;

            String q1 = parts[1];
            String q2 = parts[2];

            String c1 = clean(q1);
            String c2 = clean(q2);

            bw.write(line + "," + c1 + "," + c2);
            bw.newLine();

            count++;
        }

        br.close();
        bw.close();

        System.out.println("Preprocessing completed!");
        System.out.println("Rows processed: " + count);
        System.out.println("Output file: " + output);
    }
}