package com.uddernetworks.holysheet.compression.implementations;

import com.uddernetworks.holysheet.compression.CompressionAlgorithm;
import com.uddernetworks.holysheet.compression.CompressionFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

/**
 * @author Chris L R.
 * @version Apr 13, 2020
 */
public abstract class AbstractCompressionAlgorithm implements CompressionAlgorithm {

    /**
     * Create a temporary file in the same directory as a given file,
     * that will be deleted upon exit of the program.
     *
     * @param file {@link File}'s directory to create the temporary file in.
     * @return {@link Path} to the file.
     * @throws IOException {@link Files#createTempFile(Path, String, String, FileAttribute[])}.
     */
    protected Path createTempFile(File file) throws IOException {
        final String name = getName();
        Path path = Path.of(file.getAbsolutePath()).getParent();

        Path tempPath = Files.createTempFile(path, "holysheet-", "-" + name);
        tempPath.toFile().deleteOnExit();

        return tempPath;
    }

    /**
     * Create a temporary directory in the same directory as a given file,
     * that will be deleted upon exit of the program.
     *
     * @param file {@link File}'s directory to create the temporary directory in.
     * @return {@link Path} to the directory.
     * @throws IOException {@link Files#createTempDirectory(Path, String, FileAttribute[])}.
     */
    protected Path createTempDirectory(File file) throws IOException {
        final String name = getName();
        Path path = Path.of(file.getAbsolutePath()).getParent();

        Path tempPath = Files.createTempDirectory(path, "holysheet-" + name + "-");
        tempPath.toFile().deleteOnExit();

        return tempPath;
    }

    @Override
    public Path decompressToTemp(File file) {
        try {
            Path tempPath = createTempFile(file);

            if (decompressFile(file, tempPath)) {
                return tempPath;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean decompressFile(File file) {
        Path tempPath = decompressToTemp(file);

        if (tempPath != null) {
            File tempFile = tempPath.toFile();
            return file.delete() && tempFile.renameTo(file);
        }

        return false;
    }

    @Override
    public Path compressToTemp(File file) {
        try {
            Path tempPath = createTempFile(file);

            if (compressFile(file, tempPath)) {
                return tempPath;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean compressFile(File file) {
        Path tempPath = compressToTemp(file);

        if (tempPath != null) {
            File tempFile = tempPath.toFile();
            return file.delete() && tempFile.renameTo(file);
        }

        return false;
    }

    public String getName() {
        return CompressionFactory.prettyName(getCompressionType());
    }

    @Override
    public String toString() {
        return "CompressionAlgorithm{" + getName() + "}";
    }
}
