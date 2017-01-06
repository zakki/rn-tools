import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

public class Shuf2 {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		int n = 10;
		File outFile = new File("v.txt");
		Map<File, List<File>> dirFiles = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			if ("-n".equals(args[i])) {
				i++;
				n = Integer.parseInt(args[i]);
				continue;
			}
			if ("-o".equals(args[i])) {
				i++;
				outFile = new File(args[i]);
				continue;
			}
			String filename = args[i];
			File file = new File(filename);
			if (file.isDirectory()) {
				for (File f : file.listFiles()) {
					if (f.getName().endsWith(".gz")
							||f.getName().endsWith(".xz")
							||f.getName().endsWith(".txt")) {
						List<File> files = dirFiles.get(file);
						if (files == null) {
							files = new ArrayList<>();
							dirFiles.put(file, files);
						}
						files.add(f);
					}
				}
			}
		}

		System.err.println("n: " + n + ", out:" + outFile);

		Random r = new Random();

		ArrayList<File> files = new ArrayList<File>();
		for (int i = 0; i < n; i++) {
			Set<File> keys = dirFiles.keySet();
			for (File key : keys) {
				for (int j = 0; j < 100; j++) { 
					List<File> list = dirFiles.get(key);
					File f = list.get(r.nextInt(list.size()));
					if (!files.contains(f) || j == 99) {
						files.add(f);
						break;
					}
				}
			}
		}
		List<BufferedReader> ins = new ArrayList<>();
		Map<BufferedReader, String> inMap = new HashMap<>();
		for (File f : files) {
			if (f.getName().endsWith(".gz"))
				ins.add(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f)))));
			else if (f.getName().endsWith(".xz"))
				ins.add(new BufferedReader(new InputStreamReader(new XZCompressorInputStream(new FileInputStream(f)))));
			else if (f.getName().endsWith(".txt"))
				ins.add(new BufferedReader(new InputStreamReader(new FileInputStream(f))));
			else
				continue;
			inMap.put(ins.get(ins.size() - 1), f.getName());
		}

		System.out.println("read " + files.size());
		PrintStream out = new PrintStream(new FileOutputStream(outFile));

		ArrayList<String> buf = new ArrayList<>();

		long num = 0;
		while (ins.size() > 0) {
			BufferedReader in = ins.get(r.nextInt(ins.size()));
			String line = in.readLine();
			if (line == null) {
				in.close();
				ins.remove(in);
				System.out.println("done " + inMap.get(in) + " " + ins.size());
//				continue;
				break;
			}
			num++;
			if (num % (files.size() * 1000) == 0) {
				System.out.println(buf.size() + " " + num / files.size());
			}
			if (line.indexOf("nan") >= 0)
				continue;
			if (!line.startsWith("|win ")) {
				System.err.println("broken line " + inMap.get(in) + "::" + line);
				continue;
			}
//			int n = line.indexOf("|feature2");
//			if (n > 0)
//				out.println(line.substring(0, n));
//			else
			buf.add(line);
			if (buf.size() > 100000) {
//				Thread.sleep(1000);
				writeBuffer(out, buf);
			}
		}
		writeBuffer(out, buf);

		out.close();
	}

	private static void writeBuffer(PrintStream out, ArrayList<String> buf) {
		Collections.shuffle(buf);
		for (String l : buf) {
			out.println(l);
		}
		buf.clear();
	}
}
