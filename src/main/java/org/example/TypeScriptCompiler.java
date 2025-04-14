package org.example;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.FileSystem;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;


public class TypeScriptCompiler {

    public static void main(String[] args) throws Exception {
        Map<String, byte[]> fileSystemMap = new HashMap<>();
        FileSystem memoryFileSystem = new InMemoryFileSystem(fileSystemMap);

        Context context = Context.newBuilder("js")
                .allowIO(true)
                .allowAllAccess(true)
                .fileSystem(memoryFileSystem)
                .build();

        String typescriptCode = new String(Files.readAllBytes(Paths.get("src/main/resources/typescript.js")));
        context.eval("js", typescriptCode);

        Value tsCompiler = context.getBindings("js").getMember("ts");

        // Options for transpiling utils.ts to ESM
        Value utilsOptions = context.eval("js", "({ compilerOptions: { module: 'ES2020' } })");
        String utilsTs = new String(Files.readAllBytes(Paths.get("src/main/resources/utils.ts")));
        Value resultUtils = tsCompiler.getMember("transpileModule").execute(utilsTs, utilsOptions);
        String utilsJs = resultUtils.getMember("outputText").asString();
        fileSystemMap.put("/utils.js", utilsJs.getBytes());

        // Options for transpiling main.ts to ESM
        Value mainOptions = context.eval("js", "({ compilerOptions: { module: 'ES2020' } })");
        String mainTs = new String(Files.readAllBytes(Paths.get("src/main/resources/main.ts")));
        Value resultMain = tsCompiler.getMember("transpileModule").execute(mainTs, mainOptions);
        String mainJs = resultMain.getMember("outputText").asString();
        fileSystemMap.put("/main.js", mainJs.getBytes());


        Source esmSource = Source.newBuilder("js", new String(fileSystemMap.get("/main.js")), "/main.js")
                .mimeType("application/javascript+module")
                .build();
        context.eval(esmSource);

        context.close();
    }
}