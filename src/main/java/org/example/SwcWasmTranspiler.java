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

            String swcWasmCode = new String(
                    Files.readAllBytes(Paths.get("src/main/resources/wasm.js"))
            );

            Source swcSource = Source.newBuilder("js", swcWasmCode, "wasm.js")
                    .mimeType("application/javascript+module")
                    .build();

            Value swcModule = context.eval(swcSource); // Evaluate the module


//            if (swcModule != null && swcModule.hasMembers()) {
//                System.out.println("SWC Module Members: " + swcModule.getMemberKeys());
//            } else {
//                System.out.println("SWC Module is null or has no members.");
//            }
            if (swcModule != null && swcModule.hasMembers()) {
                this.initSyncFunc = swcModule.getMember("initSync");
                this.transformSyncFunc = swcModule.getMember("transformSync");

//                if (this.initSyncFunc != null) {
//                    System.out.println("Found initSync from module: " + this.initSyncFunc);
//                } else {
//                    System.err.println("Warning: initSync not found in module exports.");
//                }
//                if (this.transformSyncFunc != null) {
//                    System.out.println("Found transformSync from module: " + this.transformSyncFunc);
//                } else {
//                    System.err.println("Warning: transformSync not found in module exports.");
//                }
            } else {
                System.err.println("Warning: Evaluated SWC module is null or has no members.");
            }

            // Load the WASM binary as a byte array
            byte[] wasmBytes = Files.readAllBytes(Paths.get("src/main/resources/wasm_bg.wasm"));
            context.getBindings("js").putMember("wasmBytes", wasmBytes);

            // Instantiate the WebAssembly.Module using the global variable
            Value wasmModuleObj = context.eval("js", "new WebAssembly.Module(new Uint8Array(wasmBytes));");

            // Initialize SWC by calling initSync with the WASM module
            if (this.initSyncFunc != null) {
                this.initSyncFunc.execute(wasmModuleObj);
            } else {
                System.err.println("Warning: initSync function is null, cannot initialize SWC.");
            }


            if (this.initSyncFunc == null || this.initSyncFunc.isNull() || this.transformSyncFunc == null || this.transformSyncFunc.isNull()) {
                throw new RuntimeException("Failed to initialize @swc/wasm-web: initSync or transformSync function missing from module exports");
            }

        } catch (IOException e) {
            throw new RuntimeException("Error reading SWC WASM files", e);
        } catch (Exception e) {
            System.err.println("Error during SWC WASM initialization: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize @swc/wasm-web", e);
        }
    }

    public String transpile(String tsCode) {
        if (this.transformSyncFunc == null) {
            throw new RuntimeException("transformSync function is not initialized.");
        }
        Value options = context.eval("js", "({ module: { type: 'es6' }, jsc: { parser: { syntax: 'typescript' } } })");
        Value result = this.transformSyncFunc.execute(tsCode, options, context.asValue((Object) null));
        if (result.hasMember("code")) {
            return result.getMember("code").asString();
        } else {
            throw new RuntimeException("SWC WASM transpilation failed: " + result);
        }
    }
}