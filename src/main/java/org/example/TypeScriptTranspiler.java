package org.example;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class TypeScriptTranspiler {

    private final Value tsCompiler;
    private final Value options;

    public TypeScriptTranspiler(Context context) {
        try {
            String typescriptCode = new String(
                    java.nio.file.Files.readAllBytes(
                            java.nio.file.Paths.get("src/main/resources/typescript.js")
                    )
            );
            context.eval("js", typescriptCode);

            Value bindings = context.getBindings("js");
            this.tsCompiler = bindings.getMember("ts");
            this.options = context.eval("js", "({ compilerOptions: { module: 'ES2020' } })");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TypeScript transpiler", e);
        }
    }

    public String transpile(String tsCode) {
        Value result = tsCompiler.invokeMember("transpileModule", tsCode, options);
        return result.getMember("outputText").asString();
    }
}

