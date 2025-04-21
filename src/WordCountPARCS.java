package src;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import parcs.AM;        // Algorithmic Module — інтерфейс для паралельного модуля
import parcs.AMInfo;    // Контекст виконання AM: аргументи, канали, task
import parcs.channel;   // Канал для обміну даними між точками
import parcs.point;     // «Точка» обчислень — окремий процес
import parcs.task;      // Task — головна точка запуску та координації

public class WordCountPARCS implements AM {

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("WordCountPARCS <inputFile> <numWorkers>");
      System.exit(1);
    }
    String inputFile = args[0];
    int k = Integer.parseInt(args[1]);

    System.out.println(">>> START main: input=" + inputFile + ", workers=" + k);
    long globalStart = System.nanoTime();

    String text = Files.readString(Paths.get(inputFile), StandardCharsets.UTF_8);
    String[] lines = text.split("\\r?\\n");
    int chunkSize = (lines.length + k - 1) / k;

    // Створюємо Task та реєструємо наш JAR із кодом
    task curtask = new task();
    curtask.addJarFile("WordCountPARCS.jar");
    AMInfo info = new AMInfo(curtask, null);

    // Масив каналів для обміну з воркерами
    channel[] channels = new channel[k];

    // Розподіл роботи: створюємо точки, шлемо їм текстові чанки
    for (int i = 0; i < k; i++) {
      point p = info.createPoint(); // нова «точка» виконання (процес)
      channel c = p.createChannel(); // канал між master та цим воркером
      p.execute("src.WordCountPARCS"); // запускаємо AM на воркері

      // Формуємо свою порцію рядків
      int start = i * chunkSize;
      int end = Math.min(start + chunkSize, lines.length);
      StringBuilder chunk = new StringBuilder();
      for (int j = start; j < end; j++) {
        chunk.append(lines[j]).append("\n");
      }
      c.write(chunk.toString()); // відправляємо рядки через канал
      channels[i] = c; // зберігаємо канал для відповіді
    }

    // Збір результатів: кожен воркер повертає свою мапу частот
    Map<String, Integer> globalCounts = new HashMap<>();
    for (int i = 0; i < k; i++) {
      Map<String, Integer> partial =
          (Map<String, Integer>) channels[i].readObject();
      for (var e : partial.entrySet()) {
        globalCounts.merge(e.getKey(), e.getValue(), Integer::sum);
      }
    }

//    globalCounts.forEach((word, cnt) ->
//        System.out.printf("%s: %d%n", word, cnt)
//    );

    // Завершуємо Task, воркери будуть зупинені
    curtask.end();

    long globalEnd = System.nanoTime();
    System.out.printf("TOTAL elapsed: %.3f ms%n", (globalEnd-globalStart)/1e6);
  }

  @Override
  public void run(AMInfo info) {
    // Це код воркера: бере свій канал, читає рядки, рахує слова
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
