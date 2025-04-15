package org.example;


import org.graalvm.polyglot.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeScriptCompiler {

    public static void main(String[] args) throws Exception {
        // Create a map to hold the in-memory file system data (initially empty).
        Map<String, byte[]> fileSystemMap = new HashMap<>();

        // Create an instance of the custom in-memory file system.
        InMemoryFileSystem memoryFileSystem = new InMemoryFileSystem(fileSystemMap);

        // Build a GraalVM context that allows IO and uses the custom file system.
        Context context = Context.newBuilder("js")
                .allowIO(true)
                .allowAllAccess(true)
                .fileSystem(memoryFileSystem) // Provide the FileSystem during context creation
                .build();

        // Now that the context is built, we can initialize the TypeScript compiler
        // within the InMemoryFileSystem, which now has access to the context.
        memoryFileSystem.initializeTypeScriptCompiler(context);

        // We don't need to manually put main.ts into fileSystemMap anymore.

        // Before creating the Source, try to 'access' /main.js through the FileSystem.
        // This might trigger the checkAccess and transpilation.
        try {
            memoryFileSystem.checkAccess(Paths.get("/main.js"), Set.of(java.nio.file.AccessMode.READ));
        } catch (IOException e) {
            System.err.println("Error during initial access check: " + e.getMessage());
            throw e;
        }

        // Now create the GraalVM Source object to evaluate the transpiled main.js.
        byte[] mainJsBytes = fileSystemMap.get("/main.js");
        if (mainJsBytes == null) {
            throw new IllegalStateException("/main.js was not found in the file system after initial access check.");
        }
//        System.out.println("Content of mainJs :");
//        System.out.println(new String(fileSystemMap.get("/main.js")));

        Source esmSource = Source.newBuilder("js", new String(mainJsBytes), "/main.js")
                .mimeType("application/javascript+module")
                .build();

        // Evaluate the ESM source in the GraalVM context.
        context.eval(esmSource);

        // Close the GraalVM context.
        context.close();
    }
}