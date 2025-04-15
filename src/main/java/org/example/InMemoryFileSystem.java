package org.example;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class InMemoryFileSystem implements FileSystem {
    private final Map<String, byte[]> fileSystemMap;
    private Context context; // Store the context
    private Value tsCompiler;
    private Value options;

    public InMemoryFileSystem(Map<String, byte[]> fileSystemMap) {
        this.fileSystemMap = fileSystemMap;
    }

    // Separate method to initialize the TypeScript compiler after the context is built
    public void initializeTypeScriptCompiler(Context context) {
        this.context = context;
        try {
            String typescriptCode = new String(Files.readAllBytes(Paths.get("src/main/resources/typescript.js")));
            context.eval("js", typescriptCode);
            Value bindings = context.getBindings("js");
            tsCompiler = bindings.getMember("ts");
            options = context.eval("js", "({ compilerOptions: { module: 'ES2020' } })");
        } catch (IOException e) {
            throw new RuntimeException("Error loading TypeScript compiler", e);
        }
    }

    private String transpileTypeScript(String tsCode) {
        Value result = tsCompiler.invokeMember("transpileModule", tsCode, options);
        return result.getMember("outputText").asString();
    }

    @Override
    public Path parsePath(URI uri) {
        return Paths.get(uri.getPath());
    }

    @Override
    public Path parsePath(String path) {
        if (path.startsWith("/")) {
            return Paths.get(path);
        } else {
            return Paths.get("/" + path); // Ensure absolute path
        }
    }

    @Override
    public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
        String filePath = path.toString();
        if (!fileSystemMap.containsKey(filePath)) {
            if (filePath.endsWith(".js")) {
                // Construct the path to the corresponding .ts file in the resources directory
                Path resourcesPath = Paths.get("src", "main", "resources");
                String tsFileName = filePath.substring(1).replace(".js", ".ts"); // Remove leading '/'
                Path tsPath = resourcesPath.resolve(tsFileName);

                if (Files.exists(tsPath)) {
                    try {
                        String tsContent = Files.readString(tsPath);
                        String jsContent = transpileTypeScript(tsContent);
                        fileSystemMap.put(filePath, jsContent.getBytes());
                        return; // proceed
                    } catch (IOException e) {
                        throw new IOException("Error reading or transpiling TypeScript file: " + tsPath, e);
                    }
                }
            }
            throw new NoSuchFileException(filePath);
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
        // checkAccess will ensure the file exists (either originally or after transpilation)
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
        } else {
            return null;
        }
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
            if (!open) {
                throw new IOException("Channel closed");
            }
            if (position >= data.length) {
                return -1;
            }
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
            if (!open) {
                throw new IOException("Channel closed");
            }
            return position;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            if (!open) {
                throw new IOException("Channel closed");
            }
            if (newPosition < 0 || newPosition > data.length) {
                throw new IllegalArgumentException("Invalid position");
            }
            position = (int) newPosition;
            return this;
        }

        @Override
        public long size() throws IOException {
            if (!open) {
                throw new IOException("Channel closed");
            }
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