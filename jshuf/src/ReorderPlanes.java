import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

public class ReorderPlanes {

	public static void main(String[] args) throws IOException, InterruptedException {
		File inFile = new File(args[0]);
		File outFile = new File(args[1]);

		BufferedReader ins;
		Map<BufferedReader, String> inMap = new HashMap<>();

		if (inFile.getName().endsWith(".gz"))
			ins = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(inFile))));
		else if (inFile.getName().endsWith(".xz"))
			ins = new BufferedReader(new InputStreamReader(new XZCompressorInputStream(new FileInputStream(inFile))));
		else if (inFile.getName().endsWith(".txt"))
			ins = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)));
		else
			throw new IllegalArgumentException(inFile.getName());

		PrintStream out = new PrintStream(new GZIPOutputStream(new FileOutputStream(outFile)));

		final int BOARD_SIZE = 19 * 19;
		long start = System.currentTimeMillis();
		float[] features = new float[18772];
		long n = 0;
		while (true) {
			BufferedReader in = ins;
			String line = in.readLine();
			if (line == null) {
				in.close();
				System.out.println("done");
				break;
			}
			if (line.indexOf("nan") >= 0)
				continue;
			if (!line.startsWith("|win ")) {
				System.err.println("broken line " + inMap.get(in) + "::" + line);
				continue;
			}
			String[] planes = line.split("\\|");

			if (!planes[1].startsWith("win"))
				throw new IllegalArgumentException("bad win '" + planes[1] + "'");
			final int win = Integer.parseInt(planes[1].split(" ")[1]);

			if (!planes[3].startsWith("features "))
				throw new IllegalArgumentException("'" + planes[3] + "'");
			Arrays.fill(features, 0);
			parseSparse(planes[3].split(" "), features);
			final float col = features[3 * BOARD_SIZE];
			for (int i = 3 * BOARD_SIZE; i < 4 * BOARD_SIZE; i++) {
				if (features[i] != col)
					throw new IllegalArgumentException("curren color plane mismatch " + (i - 3 * BOARD_SIZE));
			}
			boolean black;
			if (col == 0) {
				black = true;
			} else if (col == 1) {
				black = false;
			} else {
				throw new IllegalArgumentException("bad color " + col);
			}
			out.print("|win ");
			out.print(black ? win : -win);
			out.print("|");
			out.print(planes[2]);
			out.print("|features ");
			if (black) {
				printSparse(out, features, BOARD_SIZE * 0, BOARD_SIZE, 0);
				printSparse(out, features, BOARD_SIZE * 1, BOARD_SIZE, BOARD_SIZE);
			} else {
				printSparse(out, features, BOARD_SIZE * 1, BOARD_SIZE, 0);
				printSparse(out, features, BOARD_SIZE * 0, BOARD_SIZE, BOARD_SIZE);
			}
			printSparse(out, features, BOARD_SIZE * 2, BOARD_SIZE * 5, BOARD_SIZE * 2);
			if (black) {
				printSparse(out, features, BOARD_SIZE * 7, BOARD_SIZE, BOARD_SIZE * 7);
				printSparse(out, features, BOARD_SIZE * 8, BOARD_SIZE, BOARD_SIZE * 8);
				printSparse(out, features, BOARD_SIZE * 9, BOARD_SIZE, BOARD_SIZE * 9);
				printSparse(out, features, BOARD_SIZE * 10, BOARD_SIZE, BOARD_SIZE * 10);
			} else {
				printSparse(out, features, BOARD_SIZE * 8, BOARD_SIZE, BOARD_SIZE * 7);
				printSparse(out, features, BOARD_SIZE * 7, BOARD_SIZE, BOARD_SIZE * 8);
				printSparse(out, features, BOARD_SIZE * 10, BOARD_SIZE, BOARD_SIZE * 9);
				printSparse(out, features, BOARD_SIZE * 9, BOARD_SIZE, BOARD_SIZE * 10);
			}
			for (int i = 0; i < 22 + 19; i++) {
				printSparse(out, features, BOARD_SIZE * (i + 11), BOARD_SIZE, BOARD_SIZE * (i + 11));
			}
			if (!planes[4].startsWith("statistic "))
				throw new IllegalArgumentException("bad statistic '" + planes[4] + "'");
			String[] ts = planes[4].split(" ");
			out.print("|statistic");
			if (black) {
				for (int i = 1; i < ts.length; i++) {
					out.print(' ');
					out.print(ts[i]);
				}
			} else {
				for (int i = 1; i < ts.length; i++) {
					out.print(' ');
					out.print(1 - Float.parseFloat(ts[i]));
				}
			}
			out.println();
			n++;
			if (n % 1000 == 0)
				System.out.println(n + " " + (System.currentTimeMillis() - start) / 1000 + "sec");
		}
		long end = System.currentTimeMillis();
		System.out.println((end - start) / 1000 + "sec");
//		writeBuffer(out, buf);

		out.close();
	}

	static void parseSparse(String[] tokens, float[] out) {
		for (int i = 1; i < tokens.length; i++) {
			String[] ts = tokens[i].split(":");
			if (ts.length != 2)
				throw new IllegalArgumentException(tokens[i]);
			out[Integer.parseInt(ts[0])] = Float.parseFloat(ts[1]);
		}
	}

	static void printSparse(PrintStream out, float[] values, int start, int size, int ofs) {
		for (int i = 0; i < size; i++) {
			if (values[i + start] != 0) {
				out.print(' ');
				out.print(i + ofs);
				out.print(':');
				if (values[i + start] == 1.0)
					out.print('1');
				else
					out.print(values[i + start]);
			}
		}
	}
}
