package org.example;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SwcWasmTranspiler {
    private final Context context;
    private Value initSyncFunc;
    private Value transformSyncFunc;

    public SwcWasmTranspiler(Context context) {
        this.context = context;

        try {
            // Load and evaluate the wasm.js module file
            String swcWasmCode = new String(Files.readAllBytes(Paths.get("src/main/resources/node_modules/@swc/wasm-web/wasm.js")));

            Source swcSource = Source.newBuilder("js", swcWasmCode, "wasm.js")
                    .mimeType("application/javascript+module")
                    .build();

            Value swcModule = context.eval(swcSource);

            if (swcModule == null || !swcModule.hasMembers()) {
                throw new RuntimeException("SWC module is null or has no members.");
            }

            this.initSyncFunc = swcModule.getMember("initSync");
            this.transformSyncFunc = swcModule.getMember("transformSync");

            if (initSyncFunc == null || initSyncFunc.isNull()) {
                throw new RuntimeException("SWC initSync function is missing or null.");
            }

            if (transformSyncFunc == null || transformSyncFunc.isNull()) {
                throw new RuntimeException("SWC transformSync function is missing or null.");
            }

            // Load the WASM binary as a byte array
            byte[] wasmBytes = Files.readAllBytes(Paths.get("src/main/resources/node_modules/@swc/wasm-web/wasm_bg.wasm"));
            context.getBindings("js").putMember("wasmBytes", wasmBytes);

            // Instantiate WebAssembly.Module
            Value initOptions = context.eval("js", "" +
                    "const wasmModuleObj = new WebAssembly.Module(new Uint8Array(wasmBytes));\n" +
                    "({ module: wasmModuleObj })"
            );

            // Initialize SWC with the wasm module
            initSyncFunc.execute(initOptions);

        } catch (IOException e) {
            throw new RuntimeException("Error reading SWC WASM files", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize @swc/wasm-web", e);
        }
    }

    public String transpile(String tsCode) {
        if (transformSyncFunc == null || transformSyncFunc.isNull()) {
            throw new IllegalStateException("SWC transformSync function is not initialized.");
        }

        Value options = context.eval("js", "({ module: { type: 'es6' }, jsc: { parser: { syntax: 'typescript' } } })");
        Value result = transformSyncFunc.execute(tsCode, options, context.asValue((Object) null));

        if (result.hasMember("code")) {
            return result.getMember("code").asString();
        } else {
            throw new RuntimeException("SWC WASM transpilation failed. Result: " + result.toString());
        }
    }
}
