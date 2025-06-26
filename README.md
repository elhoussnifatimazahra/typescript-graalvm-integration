
# TypeScript Integration Demo

This project demonstrates **TypeScript integration** in a **GraalJS-based** environment.

## Overview

The goal of this project is to enable seamless execution of **TypeScript code** inside a GraalJS runtime. It supports:

- Transpiling TypeScript to JavaScript using either **`tsc`** or **`swc`**
- Executing the resulting JavaScript with **GraalJS**
- Supporting **TypeScript modules with `import` statements**
- Benchmarking performance between **`tsc`** and **`swc`**

---

## How It Works

1. **TypeScript as Input**  
   You write `.ts` files as usual, including imports.

2. **Transpilation**  
   The `.ts` files are transpiled to `.js` using:
   - [`tsc`](https://www.typescriptlang.org/](https://www.typescriptlang.org/docs/handbook/compiler-options.html)) – the TypeScript compiler
   - [`swc`](https://swc.rs/](https://www.npmjs.com/package/@swc/wasm-web)) – a fast Rust-based transpiler

3. **Execution with GraalJS**  
   The compiled JavaScript code is executed inside a **GraalJS** context.

4. **Custom Polyglot File System**  
   - A custom virtual file system intercepts missing `.js` file requests.
   - If a matching `.ts` file exists, it is **compiled on-the-fly**.
   - The resulting `.js` is served to GraalJS as if it existed.

This simulates a Node.js-like module system, enabling full TypeScript support.

---

## Features

- Dynamic TypeScript transpilation (on-demand)
- Support for module imports in `.ts` files
- Dual transpiler support: `tsc` and `swc`
- Performance benchmarking of both compilers

---

## Benchmarks

The project includes a benchmark suite that measures:

-  Compilation time (TypeScript → JavaScript)

Here is how to run it:
```bash
mvn package
java \
  -Dcompiler=<tsc|swc> \
  -Dtrace=<true|false (defaults to false)> \
  -Dwarmup=<number of warmup iterations (defaults to 10000)> \
  --enable-native-access=org.graalvm.truffle \
  --sun-misc-unsafe-memory-access=allow \
  --module-path target/modules:target/typescript-integration-graaljs-1.0-SNAPSHOT.jar \
  --module org.example/org.example.TypeScriptCompiler
```

---


## Notes

* GraalVM must be installed and configured with `js` support.
* Make sure to allow polyglot and host access in the GraalJS context.

---
