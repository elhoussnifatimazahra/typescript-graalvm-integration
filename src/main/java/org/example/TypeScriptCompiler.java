package org.example;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.FileSystem;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;


public class TypeScriptCompiler {

    public static void main(String[] args) throws Exception {
        // Create a map to hold the in-memory file system data.
        Map<String, byte[]> fileSystemMap = new HashMap<>();
        // Create an instance of the custom in-memory file system.
        FileSystem memoryFileSystem = new InMemoryFileSystem(fileSystemMap);

        // Build a GraalVM context that allows IO and uses the custom file system.
        Context context = Context.newBuilder("js")
                .allowIO(true)
                .allowAllAccess(true)
                .fileSystem(memoryFileSystem)
                .build();

        // Read the TypeScript compiler code from a file.
        String typescriptCode = new String(Files.readAllBytes(Paths.get("src/main/resources/typescript.js")));
        // Evaluate the TypeScript compiler code in the GraalVM context, making the 'ts' object available.
        context.eval("js", typescriptCode);

        // Get the 'ts' (TypeScript) object from the JavaScript context.
        Value tsCompiler = context.getBindings("js").getMember("ts");

        // --- Transpile utils.ts ---
        // Define compiler options to output ECMAScript modules.
        Value utilsOptions = context.eval("js", "({ compilerOptions: { module: 'ES2020' } })");
        // Read the content of utils.ts.
        String utilsTs = new String(Files.readAllBytes(Paths.get("src/main/resources/utils.ts")));
        // Transpile utils.ts to JavaScript using the specified options.
        Value resultUtils = tsCompiler.getMember("transpileModule").execute(utilsTs, utilsOptions);
        // Get the transpiled JavaScript code.
        String utilsJs = resultUtils.getMember("outputText").asString();
        // Store the transpiled utils.js in the in-memory file system.
        fileSystemMap.put("/utils.js", utilsJs.getBytes());

        // --- Transpile main.ts ---
        // Define compiler options to output ECMAScript modules.
        Value mainOptions = context.eval("js", "({ compilerOptions: { module: 'ES2020' } })");
        // Read the content of main.ts.
        String mainTs = new String(Files.readAllBytes(Paths.get("src/main/resources/main.ts")));
        // Transpile main.ts to JavaScript using the specified options.
        Value resultMain = tsCompiler.getMember("transpileModule").execute(mainTs, mainOptions);
        // Get the transpiled JavaScript code.
        String mainJs = resultMain.getMember("outputText").asString();
        // Store the transpiled main.js in the in-memory file system.
        fileSystemMap.put("/main.js", mainJs.getBytes());

        // Create a GraalVM Source object to evaluate the transpiled main.js as an ECMAScript module.
        Source esmSource = Source.newBuilder("js", new String(fileSystemMap.get("/main.js")), "/main.js")
                .mimeType("application/javascript+module")
                .build();
        // Evaluate the ESM source in the GraalVM context.
        context.eval(esmSource);

        // Close the GraalVM context.
        context.close();
    }
}