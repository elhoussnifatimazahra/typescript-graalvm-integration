package org.example;

import org.graalvm.polyglot.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeScriptCompiler {

    public static void main(String[] args) throws Exception {
        String compilerChoice = "tsc"; // "tsc" or "swc"
        int warmupIterations = 5;
        int benchmarkIterations = 10;

        Map<String, byte[]> fileSystemMap = new HashMap<>();
        InMemoryFileSystem memoryFileSystem = new InMemoryFileSystem(fileSystemMap);

        Context context = Context.newBuilder("js","wasm")
                .allowIO(true)
                .allowAllAccess(true)
                .fileSystem(memoryFileSystem)
                .option("js.webassembly", "true")
                .option("js.esm-eval-returns-exports", "true")
                .option("js.text-encoding", "true")
                .build();

        memoryFileSystem.initializeCompilers(context);

        if (compilerChoice.equalsIgnoreCase("swc")) {
            memoryFileSystem.setUseSwc(true);
        } else if (compilerChoice.equalsIgnoreCase("tsc")) {
            memoryFileSystem.setUseSwc(false);
        } else {
            System.err.println("Invalid compiler choice. Using default (TypeScript).");
        }

        String mainTsContent = Files.readString(Paths.get("src/main/resources/main.ts"));
        fileSystemMap.put("/main.ts", mainTsContent.getBytes());

        System.out.printf("Warming up transpilation (%d iterations)...\n", warmupIterations);
        for (int i = 0; i < warmupIterations; i++) {
            try {
                memoryFileSystem.checkAccess(Paths.get("/main.js"), Set.of(java.nio.file.AccessMode.READ));
            } catch (IOException e) {
                System.err.println("Error during warmup iteration " + (i + 1) + ": " + e.getMessage());
                throw e;
            }
        }
        System.out.println("Warmup complete.\n");

        System.out.printf("Benchmarking transpilation (%d iterations)...\n", benchmarkIterations);
        long totalTranspilationTime = 0;
        for (int i = 0; i < benchmarkIterations; i++) {
            long startTime = System.nanoTime();
            try {
                // We need to trigger the transpilation again in each iteration
                // One way to do this is to clear the /main.js entry from the map
                fileSystemMap.remove("/main.js");
                memoryFileSystem.checkAccess(Paths.get("/main.js"), Set.of(java.nio.file.AccessMode.READ));
            } catch (IOException e) {
                System.err.println("Error during benchmark iteration " + (i + 1) + ": " + e.getMessage());
                throw e;
            }
            long endTime = System.nanoTime();
            totalTranspilationTime += (endTime - startTime);
        }

        double averageTranspilationTime = (double) totalTranspilationTime / benchmarkIterations / 1_000_000.0;
        System.out.printf("Average Warmed-up Transpilation Time (%s, %d iterations): %.3f ms%n",
                compilerChoice.toUpperCase(), benchmarkIterations, averageTranspilationTime);

        byte[] mainJsBytes = fileSystemMap.get("/main.js");
        if (mainJsBytes == null) {
            throw new IllegalStateException("/main.js was not found after benchmarking.");
        }

        Source esmSource = Source.newBuilder("js", new String(mainJsBytes), "/main.js")
                .mimeType("application/javascript+module")
                .build();

        context.eval(esmSource);
        context.close();
    }
}