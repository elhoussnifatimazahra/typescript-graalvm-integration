package org.example;

import org.graalvm.polyglot.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeScriptCompiler {

    // Choose the compiler: "tsc" or "swc"
    private static final String COMPILER_CHOICE = "tsc";
    // Number of warmup iterations before benchmarking (helps eliminate startup overhead)
    private static final int WARMUP_ITERATIONS = 900;
    // Number of iterations to run during performance benchmarking
    private static final int BENCHMARK_ITERATIONS = 1000;
    private static final String ENTRYPOINT_TS = "/index.ts";
    private static final String ENTRYPOINT_JS = "/index.js";

    // Path to the main TypeScript file to compile
    private static final Path MAIN_TS_PATH = Paths.get("src/main/resources/typescript-complex-project/src/index.ts");
    // Root directory of the TypeScript source files
    private static final Path TYPESCRIPT_SRC_PATH = Paths.get("src", "main", "resources", "typescript-complex-project", "src");

    public static void main(String[] args) throws Exception {
        // A map simulating a file system where keys are paths and values are file contents
        Map<String, byte[]> fileSystemMap = new HashMap<>();
        InMemoryFileSystem memoryFileSystem = new InMemoryFileSystem(fileSystemMap);

        // Create a GraalVM context to evaluate JS/wasm with full file system access
        Context context = Context.newBuilder("js", "wasm")
                .allowIO(true)
                .allowAllAccess(true)
                .fileSystem(memoryFileSystem)
                .option("js.webassembly", "true")
                .option("js.esm-eval-returns-exports", "true")
                .option("js.text-encoding", "true")
                .build();

        // Load TypeScript and/or SWC compilers into the memory file system
        memoryFileSystem.initializeCompilers(context);

        // Select compiler based on user choice
        if (COMPILER_CHOICE.equalsIgnoreCase("swc")) {
            memoryFileSystem.setUseSwc(true);
        } else if (COMPILER_CHOICE.equalsIgnoreCase("tsc")) {
            memoryFileSystem.setUseSwc(false);
        } else {
            System.err.println("Invalid compiler choice. Using default (TypeScript).");
        }

        // Load the main TypeScript file content into memory
        String mainTsContent = Files.readString(MAIN_TS_PATH);
        fileSystemMap.put(ENTRYPOINT_TS, mainTsContent.getBytes());

        // Walk through all TypeScript files in the source folder and load them into memory
        Files.walk(TYPESCRIPT_SRC_PATH)
                .filter(path -> path.toString().endsWith(".ts"))
                .forEach(path -> {
                    try {
                        // Convert to relative path with leading slash
                        String relativePath = "/" + TYPESCRIPT_SRC_PATH.relativize(path).toString().replace("\\", "/");
                        String content = Files.readString(path);
                        fileSystemMap.put(relativePath, content.getBytes());
                    } catch (IOException e) {
                        System.err.println("Error reading TypeScript file: " + path + " - " + e.getMessage());
                    }
                });

        // Warmup phase: run several transpilation cycles to stabilize performance
        System.out.printf("Warming up total transpilation (%d iterations)...\n", WARMUP_ITERATIONS);
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            transpileAll(memoryFileSystem, ENTRYPOINT_TS);
        }
        System.out.println("Warmup complete.\n");

        // Benchmark phase: measure total time taken to transpile over many iterations
        System.out.printf("Benchmarking total transpilation (%d iterations)...\n", BENCHMARK_ITERATIONS);
        long totalTranspilationTime = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            transpileAll(memoryFileSystem, ENTRYPOINT_TS);
            long endTime = System.nanoTime();
            totalTranspilationTime += (endTime - startTime);
        }

        // Calculate and print average transpilation time in milliseconds
        double averageTranspilationTime = (double) totalTranspilationTime / BENCHMARK_ITERATIONS / 1_000_000.0;
        System.out.printf("Average Warmed-up Total Transpilation Time (%s, %d iterations): %.3f ms%n",
                COMPILER_CHOICE.toUpperCase(), BENCHMARK_ITERATIONS, averageTranspilationTime);

        // Retrieve the transpiled JS code from memory and execute it if found
        byte[] mainJsBytes = fileSystemMap.get(ENTRYPOINT_JS);
        if (mainJsBytes == null) {
            System.err.println("Warning: " + ENTRYPOINT_JS + " was not found after transpilation. Execution might fail.");
        } else {
            // Evaluate the transpiled JavaScript as an ECMAScript module
            Source esmSource = Source.newBuilder("js", new String(mainJsBytes), ENTRYPOINT_JS)
                    .mimeType("application/javascript+module")
                    .build();
            context.eval(esmSource);
        }

        // Clean up the GraalVM context
        context.close();
    }

    // Transpiles TypeScript to JavaScript using the in-memory file system
    private static void transpileAll(InMemoryFileSystem memoryFileSystem, String entryPoint) {
        try {
            // Remove any previously generated JS files from memory
            memoryFileSystem.clearGeneratedJsFiles();

            // Trigger transpilation by attempting to read the expected JS output file
            memoryFileSystem.checkAccess(Paths.get(entryPoint.replace(".ts", ".js")), Set.of(java.nio.file.AccessMode.READ));
        } catch (IOException e) {
            System.err.println("Error during total transpilation: " + e.getMessage());
        }
    }
}
