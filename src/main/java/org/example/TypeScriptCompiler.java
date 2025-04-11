package org.example;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeScriptCompiler {
    static Map<String, byte[]> fileSystemMap = new HashMap<>();
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

    public static void main(String[] args) throws Exception {
        Map<String, byte[]> fileSystemMap = new HashMap<>();

        Context context = Context.newBuilder("js")
                .allowIO(true)
                .allowAllAccess(true)
                .option("js.commonjs-require", "true")
                .fileSystem(memoryFileSystem)
                .build();

        String typescriptCode = new String(Files.readAllBytes(Paths.get("src/main/resources/typescript.js")));
        context.eval("js", typescriptCode);

        Value tsCompiler = context.getBindings("js").getMember("ts");

        String utilsTs = new String(Files.readAllBytes(Paths.get("src/main/resources/utils.ts")));
        Value resultUtils = tsCompiler.getMember("transpileModule").execute(utilsTs, new Object());
        String utilsJs = resultUtils.getMember("outputText").asString();
        fileSystemMap.put("/utils.js", utilsJs.getBytes());


        String mainTs = new String(Files.readAllBytes(Paths.get("src/main/resources/main.ts")));
        Value resultMain = tsCompiler.getMember("transpileModule").execute(mainTs, new Object());
        String mainJs = resultMain.getMember("outputText").asString();


        mainJs = mainJs.replace("require('./utils')", "require('/utils.js')");

        fileSystemMap.put("/main.js", mainJs.getBytes());


        System.out.println("Contenu de fileSystemMap :");
        for (String path : fileSystemMap.keySet()) {
            System.out.println("- " + path);
        }

        System.out.println("Contenu de mainJs :");
        System.out.println(new String(fileSystemMap.get("/main.js")));

        context.eval("js", new String(fileSystemMap.get("/main.js")));

        context.close();
    }
    static FileSystem memoryFileSystem = new FileSystem() {
        @Override
        public Path parsePath(URI uri) {
            System.out.println("parsePath: " + uri);
            return Paths.get(uri);
        }

        @Override
        public Path parsePath(String path) {
            System.out.println("parsePath: " + path);
            return Paths.get(path);
        }

        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
            System.out.println("checkAccess: " + path);
            if (!fileSystemMap.containsKey(path.toString())) {
                throw new NoSuchFileException(path.toString());
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
            System.out.println("newByteChannel: " + path);
            System.out.println("fileSystemMap keys:" + fileSystemMap.keySet());
            if (fileSystemMap.containsKey(path.toString())) {
                return new InMemoryByteChannel(fileSystemMap.get(path.toString()));
            }
            throw new NoSuchFileException(path.toString());
        }




        public Path toAbsolutePath(URI uri) {
            return Paths.get(uri.getPath());
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
            System.out.println("toRealPath: " + path); // Log de débogage
            if (fileSystemMap.containsKey(path.toString())) {
                return path; // Return the path if the file exists
            } else {
                return null; // Return null if the file does not exist
            }
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
            return Map.of();
        }

        public Path getPath(URI uri) {
            return Paths.get(uri.getPath());
        }
    };
}