package org.example;

import org.graalvm.polyglot.*;
import java.nio.file.*;
import java.util.*;

public class TypeScriptCompiler {
    private static final Engine sharedEngine = Engine.newBuilder().option("engine.TraceCompilation", System.getProperty("trace", "false")).build();
    private static final String COMPILER_CHOICE = System.getProperty("compiler", "tsc"); // or "swc"
    private static final Path TYPESCRIPT_SRC_PATH = Paths.get("src/main/resources/typescript-project");
    private static final String ENTRYPOINT_TS = "/index.ts";
    private static final String ENTRYPOINT_JS = "/index.js";
    private static final int WARMUP_ITERATIONS = Integer.parseInt(System.getProperty("warmup", "10000"));
    private static final int BENCHMARK_ITERATIONS = 1000;



    public static void main(String[] args) throws Exception {
        Map<String, byte[]> fileSystemMap = new HashMap<>();
        InMemoryFileSystem memoryFileSystem = new InMemoryFileSystem(fileSystemMap);

        try (Context context = Context.newBuilder("js", "wasm")
                .allowAllAccess(true)
                .engine(sharedEngine)
                .fileSystem(memoryFileSystem)
                .option("js.webassembly", "true")
                .option("js.esm-eval-returns-exports", "true")
                .option("js.text-encoding", "true")
                .build()) {

            memoryFileSystem.initializeCompilers(context);
            memoryFileSystem.setUseSwc("swc".equalsIgnoreCase(COMPILER_CHOICE));
            memoryFileSystem.loadProject(TYPESCRIPT_SRC_PATH);

            TranspilationBenchmarkRunner.run(memoryFileSystem, ENTRYPOINT_TS, WARMUP_ITERATIONS, BENCHMARK_ITERATIONS);

            byte[] mainJsBytes = fileSystemMap.get(ENTRYPOINT_JS);
            if (mainJsBytes == null) {
                System.err.println("Warning: " + ENTRYPOINT_JS + " not found after transpilation.");
            } else {
                Source esmSource = Source.newBuilder("js", new String(mainJsBytes), ENTRYPOINT_JS)
                        .mimeType("application/javascript+module")
                        .build();
                context.eval(esmSource);
            }

        }
    }
}
