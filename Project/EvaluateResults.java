import java.io.*;
import java.util.*;

public class EvaluateResults {

    public static void main(String[] args) throws Exception {

        BufferedReader original = new BufferedReader(
                new FileReader("Near_Duplicate_Question_Dataset_1000.csv"));

        BufferedReader results = new BufferedReader(
                new FileReader("final_results.csv"));

        original.readLine();
        results.readLine();

        Map<String, Integer> actualLabels = new HashMap<>();

        String line;

        while ((line = original.readLine()) != null) {

            String[] parts = line.split(",", 5);

            if (parts.length >= 4) {
                String id = parts[0];
                int label = Integer.parseInt(parts[3]);

                actualLabels.put(id, label);
            }
        }

        original.close();

        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;

        while ((line = results.readLine()) != null) {

            String[] parts = line.split(",", 7);

            if (parts.length < 7)
                continue;

            String id1 = parts[0];
            String prediction = parts[6];

            int actual = actualLabels.getOrDefault(id1, 0);

            int predicted = prediction.equals("Near Duplicate") ? 1 : 0;

            if (actual == 1 && predicted == 1)
                tp++;
            else if (actual == 0 && predicted == 0)
                tn++;
            else if (actual == 0 && predicted == 1)
                fp++;
            else if (actual == 1 && predicted == 0)
                fn++;
        }

        results.close();

        int total = tp + tn + fp + fn;

        double accuracy = (double) (tp + tn) / total;

        double precision = tp + fp == 0
                ? 0
                : (double) tp / (tp + fp);

        double recall = tp + fn == 0
                ? 0
                : (double) tp / (tp + fn);

        double f1 = precision + recall == 0
                ? 0
                : 2 * precision * recall / (precision + recall);

        System.out.println("Evaluation completed.");
        System.out.println("--------------------");
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