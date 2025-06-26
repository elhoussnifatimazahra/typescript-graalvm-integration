package org.example;

public class TranspilationBenchmarkRunner {

    public static void run(InMemoryFileSystem memoryFileSystem, String entryPoint, int warmupIterations, int benchmarkIterations) {
        System.out.printf("Warming up transpilation (%d iterations)...\n", warmupIterations);
        for (int i = 0; i < warmupIterations; i++) {
            memoryFileSystem.transpile(entryPoint);
        }
        System.out.println("Warmup complete.\n");

        System.out.printf("Benchmarking transpilation (%d iterations)...\n", benchmarkIterations);
        long start = System.nanoTime();
        for (int i = 0; i < benchmarkIterations; i++) {
            memoryFileSystem.transpile(entryPoint);
        }
        long end = System.nanoTime();

        double avgMs = (double) (end - start) / benchmarkIterations / 1_000_000.0;
        System.out.printf("Average transpilation time: %.3f ms\n", avgMs);
    }
}
