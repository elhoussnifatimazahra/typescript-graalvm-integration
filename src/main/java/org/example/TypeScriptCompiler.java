// TypeScriptCompiler.java
package org.example;

import org.graalvm.polyglot.*;
import java.nio.file.*;
import java.util.*;

public class TypeScriptCompiler {
    private static final Engine sharedEngine = Engine.newBuilder().option("engine.TraceCompilation", "false").build();
    private static final String COMPILER_CHOICE = "swc"; // or "tsc"
    private static final int WARMUP_ITERATIONS = 900;
    private static final int BENCHMARK_ITERATIONS = 1000;
    private static final String ENTRYPOINT_TS = "/index.ts";
    private static final String ENTRYPOINT_JS = "/index.js";
    private static final Path TYPESCRIPT_SRC_PATH = Paths.get("src/main/resources/typescript-project");

    public static void main(String[] args) throws Exception {
        Map<String, byte[]> fileSystemMap = new HashMap<>();
        InMemoryFileSystem memoryFileSystem = new InMemoryFileSystem(fileSystemMap);

        Context context = Context.newBuilder("js", "wasm")
                .allowAllAccess(true)
                .engine(sharedEngine)
                .fileSystem(memoryFileSystem)
                .option("js.webassembly", "true")
                .option("js.esm-eval-returns-exports", "true")
                .option("js.text-encoding", "true")
                .build();

        memoryFileSystem.initializeCompilers(context);
        memoryFileSystem.setUseSwc("swc".equalsIgnoreCase(COMPILER_CHOICE));
        memoryFileSystem.loadProject(TYPESCRIPT_SRC_PATH);

        System.out.printf("Warming up transpilation (%d iterations)...\n", WARMUP_ITERATIONS);
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            memoryFileSystem.transpile(ENTRYPOINT_TS);
        }

        System.out.println("Warmup complete.\n");
        System.out.printf("Benchmarking transpilation (%d iterations)...\n", BENCHMARK_ITERATIONS);
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            memoryFileSystem.transpile(ENTRYPOINT_TS);
        }
        long end = System.nanoTime();
        double avgMs = (double) (end - start) / BENCHMARK_ITERATIONS / 1_000_000.0;
        System.out.printf("Average transpilation time (%s): %.3f ms\n", COMPILER_CHOICE.toUpperCase(), avgMs);

        byte[] mainJsBytes = fileSystemMap.get(ENTRYPOINT_JS);
        if (mainJsBytes == null) {
            System.err.println("Warning: " + ENTRYPOINT_JS + " not found after transpilation.");
        } else {
            Source esmSource = Source.newBuilder("js", new String(mainJsBytes), ENTRYPOINT_JS)
                    .mimeType("application/javascript+module")
                    .build();
            context.eval(esmSource);
        }

        context.close();
    }
}
