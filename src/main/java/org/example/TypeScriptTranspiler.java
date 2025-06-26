package org.example;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.nio.file.Paths;

public class TypeScriptTranspiler {

    private final TypeScriptCompiler tsCompiler;
    private final JSObject options;

    public TypeScriptTranspiler(Context context) {
        try {
            var typescriptSource = Source.newBuilder("js", Paths.get("src/main/resources/node_modules/typescript/lib/typescript.js").toUri().toURL()).build();
            context.eval(typescriptSource);

            Value bindings = context.getBindings("js");
            this.tsCompiler = bindings.getMember("ts").as(TypeScriptCompiler.class);
            this.options = context.eval("js", "({ compilerOptions: { module: 'ES2020' } })").as(JSObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TypeScript transpiler", e);
        }
    }

    public String transpile(String tsCode) {
        return tsCompiler.transpileModule(tsCode, options).outputText();
    }

    public interface TypeScriptCompiler {
        TypeScriptCompilerResult transpileModule(String tsCode, JSObject options);
    }

    public interface TypeScriptCompilerResult {
        String outputText();
    }

    public interface JSObject {
    }
}

