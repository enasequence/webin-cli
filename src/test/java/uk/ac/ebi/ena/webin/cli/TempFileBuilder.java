package uk.ac.ebi.ena.webin.cli;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;

public class TempFileBuilder {
    private Path dir;
    private String name = "TEST";
    private String contents;
    private boolean gzip = false;

    public static Path empty() {
        return new TempFileBuilder().build();
    }

    public static Path empty(String name) {
        return new TempFileBuilder().name(name).build();
    }

    public static Path file(String contents) {
        return new TempFileBuilder()
                .contents(contents)
                .build();
    }

    public static Path file(String name, String contents) {
        return new TempFileBuilder()
                .name(name)
                .contents(contents)
                .build();
    }

    public static Path file(Path dir, String contents) {
        return new TempFileBuilder()
                .dir(dir)
                .contents(contents)
                .build();
    }

    public static Path file(Path dir, String name, String contents) {
        return new TempFileBuilder()
                .dir(dir)
                .name(name)
                .contents(contents)
                .build();
    }

    public static Path gzip(String name, String contents) {
        return new TempFileBuilder()
                .name(name)
                .contents(contents)
                .gzip()
                .build();
    }

    public static Path gzip(Path dir, String name, String contents) {
        return new TempFileBuilder()
                .dir(dir)
                .name(name)
                .contents(contents)
                .gzip()
                .build();
    }

    public TempFileBuilder dir(Path dir) {
        this.dir = dir;
        return this;
    }

    public TempFileBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TempFileBuilder contents(String contents) {
        this.contents = contents;
        return this;
    }

    public TempFileBuilder gzip() {
        this.gzip = true;
        return this;
    }

    public Path
    build() {
        try {
            Path path;
            if (dir != null) {
                path = Files.createTempFile(dir, "TEST", name);
            } else {
                path = Files.createTempFile("TEST", name);
            }
            if (contents != null) {
                InputStream is = new ByteArrayInputStream(contents.getBytes());
                OutputStream os;
                IOUtils.copy(is, (os = gzip ?
                        new GZIPOutputStream(
                                Files.newOutputStream(path,
                                        StandardOpenOption.TRUNCATE_EXISTING,
                                        StandardOpenOption.SYNC))
                        : Files.newOutputStream(path,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.SYNC)));
                os.flush();
                os.close();
            }
            Assert.assertTrue(Files.exists(path));
            Assert.assertTrue(Files.isRegularFile(path));
            return path;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
