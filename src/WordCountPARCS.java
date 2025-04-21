package src;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import parcs.AM;
import parcs.AMInfo;
import parcs.channel;
import parcs.point;
import parcs.task;

public class WordCountPARCS implements AM {
  private static long distStart;
  private static long collStart;

  static void startDist() {
    distStart = System.nanoTime();
  }

  static void stopDist() {
    long distEnd = System.nanoTime();
    System.out.printf("Distribution time: %.3f ms%n", (distEnd - distStart) / 1e6);
  }

  static void startColl() {
    collStart = System.nanoTime();
  }

  static void stopColl() {
    long collEnd = System.nanoTime();
    System.out.printf("Collection+Merge time: %.3f ms%n", (collEnd - collStart) / 1e6);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("WordCountPARCS <inputFile> <numWorkers>");
      System.exit(1);
    }
    String inputFile = args[0];
    int k = Integer.parseInt(args[1]);

    String text = Files.readString(Paths.get(inputFile), StandardCharsets.UTF_8);
    String[] lines = text.split("\\r?\\n");
    int chunkSize = (lines.length + k - 1) / k;

    task curtask = new task();
    curtask.addJarFile("WordCountPARCS.jar");
    AMInfo info = new AMInfo(curtask, null);

    channel[] channels = new channel[k];

    startDist();
    for (int i = 0; i < k; i++) {
      point p = info.createPoint();
      channel c = p.createChannel();
      p.execute("src.WordCountPARCS");

      int start = i * chunkSize;
      int end = Math.min(start + chunkSize, lines.length);
      StringBuilder chunk = new StringBuilder();
      for (int j = start; j < end; j++) {
        chunk.append(lines[j]).append("\n");
      }
      c.write(chunk.toString());
      channels[i] = c;
    }
    stopDist();

    startColl();
    Map<String, Integer> globalCounts = new HashMap<>();
    for (int i = 0; i < k; i++) {
      Map<String, Integer> partial =
          (Map<String, Integer>) channels[i].readObject();
      for (var e : partial.entrySet()) {
        globalCounts.merge(e.getKey(), e.getValue(), Integer::sum);
      }
    }
    stopColl();

    globalCounts.forEach((word, cnt) ->
        System.out.printf("%s: %d%n", word, cnt)
    );

    curtask.end();
  }

  @Override
  public void run(AMInfo info) {
    String chunk = (String) info.parent.readObject();
    Map<String, Integer> counts = new HashMap<>();
    for (String token : chunk.split("\\W+")) {
      if (token.isEmpty()) {
        continue;
      }
      counts.merge(token.toLowerCase(), 1, Integer::sum);
    }
    info.parent.write((Serializable) counts);
  }
}
