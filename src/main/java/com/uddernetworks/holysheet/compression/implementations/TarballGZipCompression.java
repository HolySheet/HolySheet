package com.uddernetworks.holysheet.compression.implementations;

import com.uddernetworks.grpc.HolysheetService;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This can be counted as compression, as it also compresses the tarball with gzip ;)
 * Influence was taken from stack-overflow user: pierrefevrier; his idea of
 * {@link Files#walk(Path, FileVisitOption...)} and using {@link StringUtils#substringAfter(String, String)}.
 *
 * @author Chris L R.
 * @version Apr 13, 2020
 */
public class TarballGZipCompression extends AbstractCompressionAlgorithm {

    @Override
    public HolysheetService.UploadRequest.Compression getCompressionType() {
        return HolysheetService.UploadRequest.Compression.TARBALL_GZIP;
    }

    /**
     * Write a tar entry from a {@link TarArchiveInputStream} to a given {@link File}.
     *
     * @param stream {@link TarArchiveInputStream} being read from.
     * @param file   {@link File} (pre-created) to transfer the data to.
     * @throws IOException {@link InputStream#transferTo(OutputStream)} can throw.
     */
    private void writeTarFileEntry(TarArchiveInputStream stream, File file) throws IOException {
        try (final var fileOut = new BufferedOutputStream(new FileOutputStream(file))) {
            stream.transferTo(fileOut);
        }
    }

    /**
     * Unarchive and decompress a given tarball, compressed with gzip, to a given
     * destination folder.
     *
     * @param file        {@link File} of gzipped tarball, for example "tarball.tar.gz"
     * @param destination {@link Path} to destination folder.
     * @throws IOException This whole method is filled with possible IOExceptions...
     */
    private void internalDecompress(File file, Path destination) throws IOException {
        final var fileInput = new BufferedInputStream(new FileInputStream(file));
        final String exception = "Could not make directories whilst un-tarring archive, ";

        try (final var tarInput = new TarArchiveInputStream(new GZIPInputStream(fileInput))) {
            TarArchiveEntry entry;

            while ((entry = tarInput.getNextTarEntry()) != null) {
                final File entryFile = destination.resolve(entry.getName()).toFile();

                if (entry.isDirectory()) {
                    if (!entryFile.mkdirs()) {
                        throw new IOException(exception + entryFile.getAbsolutePath());
                    }
                } else {
                    entryFile.getParentFile().mkdirs();
                    entryFile.createNewFile();

                    writeTarFileEntry(tarInput, entryFile);
                }
            }
        }
    }

    /**
     * Add a given {@link File}, with a given <em>relative</em> {@link String} path, to a
     * {@link TarArchiveOutputStream}, by creating a {@link TarArchiveEntry}.
     *
     * @param file      {@link File} to add to the tarball.
     * @param path      relative path of the file.
     * @param tarOutput {@link TarArchiveOutputStream} stream to write to.
     * @throws IOException This whole method, once again, is a trap for IOExceptions.
     */
    private void addToTarball(File file, String path, TarArchiveOutputStream tarOutput) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(file, path);

        try (final var fileInput = new BufferedInputStream(new FileInputStream(file))) {
            tarOutput.putArchiveEntry(entry);
            fileInput.transferTo(tarOutput);

            tarOutput.closeArchiveEntry();
        }
    }

    /**
     * Archive, then compress with gzip, a given folder, and store it in a given destination.
     *
     * @param directory   {@link File} directory to archive & compress (gzip).
     * @param destination {@link Path} to the destination of this compressed tarball.
     * @throws IOException                   Again, this method is a trap for IOExceptions.
     * @throws UnsupportedOperationException If a non-directory {@link File} is passed.
     */
    private void internalCompress(File directory, Path destination) throws IOException {
        if (!directory.isDirectory()) {
            throw new UnsupportedOperationException("Cannot tarball a non-directory file.");
        }

        final var fileOut = new BufferedOutputStream(new FileOutputStream(destination.toFile()));
        final String directoryPath = directory.getPath();

        try (final var tarOut = new TarArchiveOutputStream(new GZIPOutputStream(fileOut))) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            var files = Files.walk(Path.of(directoryPath))
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .collect(Collectors.toUnmodifiableList());

            // for every folder, add it to the tarball...
            for (File file : files) {
                String relativePath = StringUtils.substringAfter(file.getPath(), directoryPath);
                addToTarball(file, relativePath, tarOut);
            }

            tarOut.flush();
        }
    }

    @Override
    public boolean decompressFile(File file, Path destination) {
        if (!file.exists()) {
            return false;
        }

        try {
            internalDecompress(file, destination);
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean compressFile(File file, Path destination) {
        if (!file.exists()) {
            return false;
        }

        try {
            internalCompress(file, destination);
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    /**
     * This method was overriden to make sure that a temporary <em>directory</em> was created,
     * not a temporary file.
     *
     * @param file {@link File} tarball to decompress & unarchive.
     * @return {@link Path} to decompressed & unarchived folder.
     */
    @Override
    public Path decompressToTemp(File file) {
        try {
            Path tempPath = createTempDirectory(file); // this differs by creating a directory rather than a file.

            if (decompressFile(file, tempPath)) {
                return tempPath;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

}
