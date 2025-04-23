package org.example;

import org.graalvm.polyglot.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeScriptCompiler {

    public static void main(String[] args) throws Exception {
        String compilerChoice = "swc"; // "tsc" or "swc"

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

        long startTime = System.nanoTime();
        try {
            memoryFileSystem.checkAccess(Paths.get("/main.js"), Set.of(java.nio.file.AccessMode.READ));
        } catch (IOException e) {
            System.err.println("Error during initial access check: " + e.getMessage());
            throw e;
        }
        long endTime = System.nanoTime();
        System.out.printf("Total Transpilation Time (%s): %.3f ms%n",
                compilerChoice.toUpperCase(), (endTime - startTime) / 1_000_000.0);

        byte[] mainJsBytes = fileSystemMap.get("/main.js");
        if (mainJsBytes == null) {
            throw new IllegalStateException("/main.js was not found after transpilation.");
        }

        Source esmSource = Source.newBuilder("js", new String(mainJsBytes), "/main.js")
                .mimeType("application/javascript+module")
                .build();

        context.eval(esmSource);
        context.close();
    }
}