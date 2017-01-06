import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Shuf3 {
	
	public static void main(String[] args) throws Exception {
		ArrayList<File> files = new ArrayList<File>();
		for (String filename : args) {
			File file = new File(filename);
			if (file.isDirectory()) {
				for (File f : file.listFiles()) {
					if (f.getName().endsWith(".gz")
							||f.getName().endsWith(".txt")) {
						files.add(f);
					}
				}
			}
		}

		List<BufferedReader> ins = new ArrayList<>();
		Map<BufferedReader, String> inMap = new HashMap<>();
		for (File f : files) {
			if (f.getName().endsWith(".gz"))
				ins.add(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f)))));
			else if (f.getName().endsWith(".txt"))
				ins.add(new BufferedReader(new InputStreamReader(new FileInputStream(f))));
			else
				continue;
			inMap.put(ins.get(ins.size() - 1), f.getName());
		}

		System.out.println("read " + files.size());

		OutBuf[] bufs = new OutBuf[4];
		for (int i = 0; i < bufs.length; i++) {
			bufs[i] = new OutBuf(i);
		}

		LinkedBlockingQueue<String> buf = new LinkedBlockingQueue<>();
		Thread[] threads = new Thread[4];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				Random r2 = new Random();
				while (true) {
					String line = buf.poll();
					if (line == null) {
						try {
//							System.out.print(".");
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						if (line.length() == 0)
							break;
						int len = getGameLength(line);
						OutBuf ob = bufs[Math.min(3, len / 50)];
						int n = ob.n.incrementAndGet();
						if (n % 10000 == 0)
							System.out.println((len / 50) + ":" + n);
						PrintStream out = ob.outs.get(r2.nextInt(ob.outs.size()));
						synchronized (out) {
							out.println(line);
						}
					}
				}
			});
			threads[i].start();
		}

		long num = 0;
		Random r = new Random();
		while (ins.size() > 0) {
			BufferedReader in = ins.get(r.nextInt(ins.size()));
			String line = in.readLine();
			if (line == null) {
				in.close();
				ins.remove(in);
				System.out.println("done " + ins.size());
				continue;
			}
			num++;
			if (num % (files.size() * 1000) == 0) {
				System.out.println(buf.size() + " " + num / files.size());
			}
//			if (line.indexOf("nan") >= 0)
//				continue;
//			if (!line.startsWith("|win ")) {
//				System.err.println("broken line " + inMap.get(in) + "::" + line);
//				continue;
//			}
//			int n = line.indexOf("|feature2");
//			if (n > 0)
//				out.println(line.substring(0, n));
//			else
			buf.add(line);
			if (buf.size() > 10000) {
				Thread.sleep(10);
				System.out.print("-");
			}
		}

		for (int i = 0; i < threads.length; i++) {
			buf.add("");
		}
		for (Thread thread : threads)
			thread.join();

		for (OutBuf ob : bufs) {
			for (PrintStream out : ob.outs) {
				out.close();
			}
		}
	}

	static int getGameLength(String line) {
		line = line.substring(line.indexOf("features") + 9);
//		System.out.println(line);
		int n = 0;
		try {
			Scanner s = new Scanner(line);
			s.useDelimiter("(\\s|:)");
			while (s.hasNext()) {
				int p = s.nextInt();
				if (p >= 19 * 19 * 2)
					break;
				n++;
				s.skip(":");
				s.nextInt();
			}
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return n;
	}

	static class OutBuf {
		AtomicInteger n = new AtomicInteger();
		List<PrintStream> outs = new ArrayList<>();
		LinkedBlockingQueue<String> buf = new LinkedBlockingQueue<>();

		OutBuf(int dir) throws Exception {
			new File("" + dir).mkdir();
//			for (int i = 0; i < 100; i++) {
			for (int i = 0; i < 1; i++) {
				System.out.println(String.format("shuf_%03d.gz", i));
//				outs.add(new PrintStream(new GZIPOutputStream(new FileOutputStream(new File(String.format("%d/shuf_%03d.gz", dir, i))))));
				outs.add(new PrintStream(new FileOutputStream(new File(String.format("%d/shuf_%03d.txt", dir, i)))));
//				outs.add(new PrintStream(new XZCompressorOutputStream(new FileOutputStream(new File(String.format("shuf_%03d.txt.xz", i))), 3)));
			}
		}
	}
}
