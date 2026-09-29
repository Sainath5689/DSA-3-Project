
import java.io.*;
import java.util.*;

public class DoubleHashing {

    static final long MOD1 = 1000000007;
    static final long MOD2 = 1000000009;
    static final long BASE = 31;

    static long hash(String s, long mod) {

        long h = 0;

        for (int i = 0; i < s.length(); i++) {
            h = (h * BASE + s.charAt(i)) % mod;
        }

        return h;
    }

    public static void main(String[] args) throws IOException {

        BufferedReader br = new BufferedReader(
                new FileReader("shingles.csv"));

        BufferedWriter bw = new BufferedWriter(
                new FileWriter("hashed_shingles.csv"));

        String line;
        int count = 0;

        br.readLine(); // Skip header

        bw.write("id,question_1_hashes,question_2_hashes");
        bw.newLine();

        while ((line = br.readLine()) != null) {

            String[] parts = line.split(",", 3);

            if (parts.length < 3)
                continue;

            String id = parts[0];
            String q1 = parts[1].replace("\"", "");
            String q2 = parts[2].replace("\"", "");

            StringBuilder h1 = new StringBuilder();
            StringBuilder h2 = new StringBuilder();

            for (String shingle : q1.split("\\s*\\|\\s*")) {

                if (shingle.isEmpty())
                    continue;

                h1.append(hash(shingle, MOD1))
                  .append(":")
                  .append(hash(shingle, MOD2))
                  .append(" | ");
            }

            for (String shingle : q2.split("\\s*\\|\\s*")) {

                if (shingle.isEmpty())
                    continue;

                h2.append(hash(shingle, MOD1))
                  .append(":")
                  .append(hash(shingle, MOD2))
                  .append(" | ");
            }

            bw.write(id + ",\"" + h1 + "\",\"" + h2 + "\"");
            bw.newLine();

            count++;
        }

        br.close();
        bw.close();

        System.out.println("Double hashing completed!");
        System.out.println("Rows processed: " + count);
        System.out.println("Output file: hashed_shingles.csv");
    }
}