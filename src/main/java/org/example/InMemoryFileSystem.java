package org.example;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.io.FileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.stream.Stream;


class InMemoryFileSystem implements FileSystem {
    private final Map<String, byte[]> fileSystemMap;
    private final Set<String> generatedJsFiles = new HashSet<>();
    private TypeScriptTranspiler typeScriptTranspiler;
    private SwcWasmTranspiler swcWasmTranspiler;
    private boolean useSwc = false;

    public InMemoryFileSystem(Map<String, byte[]> fileSystemMap) {
        this.fileSystemMap = fileSystemMap;
    }

    public void setUseSwc(boolean useSwc) {
        this.useSwc = useSwc;
    }

    public void initializeCompilers(Context context) {
        this.typeScriptTranspiler = new TypeScriptTranspiler(context);
        try {
            this.swcWasmTranspiler = new SwcWasmTranspiler(context);
        } catch (RuntimeException e) {
            System.err.println("Warning: Failed to initialize @swc/wasm-web. Using TypeScript compiler. Error: " + e.getMessage());
            this.useSwc = false;
        }
    }

    public void loadProject(Path sourcePath) throws IOException {
        try (Stream<Path> paths = Files.walk(sourcePath)) {
                paths.filter(path -> path.toString().endsWith(".ts"))
                    .forEach(path -> {
                        try {
                            String relativePath = "/" + sourcePath.relativize(path).toString().replace("\\", "/");
                            String content = Files.readString(path);
                            fileSystemMap.put(relativePath, content.getBytes());
                        } catch (IOException e) {
                            System.err.println("Error reading TypeScript file: " + path + " - " + e.getMessage());
                        }
                    });
        }
    }

    public void transpile(String entryPoint) {
        try {
            clearGeneratedJsFiles();
            checkAccess(Paths.get(entryPoint.replace(".ts", ".js")), Set.of(AccessMode.READ));
        } catch (IOException e) {
            System.err.println("Error during transpilation: " + e.getMessage());
        }
    }

    public void clearGeneratedJsFiles() {
        generatedJsFiles.forEach(fileSystemMap::remove);
        generatedJsFiles.clear();
    }

    @Override
    public Path parsePath(URI uri) {
        return Paths.get(uri.getPath());
    }

    @Override
    public Path parsePath(String path) {
        return Paths.get(path.startsWith("/") ? path : "/" + path);
    }

    @Override
    public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
        String filePath = path.toString();
        if (!fileSystemMap.containsKey(filePath)) {
            if (filePath.endsWith(".js")) {
                String tsFilePath = filePath.replace(".js", ".ts");
                if (fileSystemMap.containsKey(tsFilePath)) {
                    String tsContent = new String(fileSystemMap.get(tsFilePath));
                    String transpiled = useSwc && swcWasmTranspiler != null
                            ? swcWasmTranspiler.transpile(tsContent)
                            : typeScriptTranspiler.transpile(tsContent);
                    fileSystemMap.put(filePath, transpiled.getBytes());
                    generatedJsFiles.add(filePath);
                    return;
                }
            }
            throw new NoSuchFileException("No .js or .ts file found for: " + filePath);
        }
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("createDirectory is not supported in memory filesystem");
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException("delete is not supported in memory filesystem");
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        String filePath = path.toString();
        checkAccess(path, Set.of(AccessMode.READ));
        if (fileSystemMap.containsKey(filePath)) {
            return new InMemoryByteChannel(fileSystemMap.get(filePath));
        }
        throw new NoSuchFileException(filePath);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null;
    }

    @Override
    public Path toAbsolutePath(Path path) {
        return path;
    }

    @Override
    public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
        String filePath = path.toString();
        if (fileSystemMap.containsKey(filePath) || Files.exists(Paths.get(filePath.replace(".js", ".ts")))) {
            return path;
        }
        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return Map.of();
    }

    private static class InMemoryByteChannel implements SeekableByteChannel {
        private final byte[] data;
        private int position;
        private boolean open = true;

        public InMemoryByteChannel(byte[] data) {
            this.data = data;
            this.position = 0;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            if (!open) throw new IOException("Channel closed");
            if (position >= data.length) return -1;
            int bytesToRead = Math.min(dst.remaining(), data.length - position);
            dst.put(data, position, bytesToRead);
            position += bytesToRead;
            return bytesToRead;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            throw new UnsupportedOperationException("Write is not supported for this memory channel");
        }

        @Override
        public long position() throws IOException {
            if (!open) throw new IOException("Channel closed");
            return position;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            if (!open) throw new IOException("Channel closed");
            if (newPosition < 0 || newPosition > data.length) throw new IllegalArgumentException("Invalid position");
            position = (int) newPosition;
            return this;
        }

        @Override
        public long size() throws IOException {
            if (!open) throw new IOException("Channel closed");
            return data.length;
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            throw new UnsupportedOperationException("Truncate is not supported for this memory channel");
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws IOException {
            open = false;
        }
    }
}
