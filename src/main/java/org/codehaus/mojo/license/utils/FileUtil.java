package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2010 - 2011 Codehaus
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

/**
 * Some basic file io utilities
 *
 * @author pgier
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public class FileUtil {

    /**
     * Rename the given file to a new destination.
     *
     * @param file        the file to rename
     * @param destination the destination file
     * @throws IOException if could not rename the file
     */
    public static void renameFile(File file, File destination) throws IOException {
        try {
            try {
                FileUtils.forceDelete(destination);
            } catch (FileNotFoundException ex) {
                // Just do nothing
            }

            FileUtils.moveFile(file, destination);
        } catch (IOException ex) {
            throw new IOException(String.format("could not rename '%s' to '%s'", file, destination));
        }
    }

    /**
     * Copy a file to a given locationand logging.
     *
     * @param source represents the file to copy.
     * @param target file name of destination file.
     * @throws IOException if could not copy file.
     */
    public static void copyFile(File source, File target) throws IOException {
        Files.createDirectories(target.getParentFile().toPath());
        FileUtils.copyFile(source, target);
    }

    /**
     * @param file the source file
     * @return the backup file
     */
    public static File getBackupFile(File file) {
        return new File(file.getAbsolutePath() + "~");
    }

    /**
     * Backups the given file using the {@link FileUtil#getBackupFile(File)} as
     * destination file.
     *
     * @param f the file to backup
     * @throws IOException if any pb while copying the file
     */
    public static void backupFile(File f) throws IOException {
        File dst = getBackupFile(f);
        copyFile(f, dst);
    }

    /**
     * Print content to file. This method ensures that a platform specific line ending is used.
     *
     * @param file     the file to write to
     * @param content  the content to write
     * @param encoding the encoding to write in
     * @throws IOException if IO pb
     */
    public static void printString(File file, String content, String encoding) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());

        try (BufferedReader in = new BufferedReader(new StringReader(content));
                PrintWriter out = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding)))) {
            String line;
            while ((line = in.readLine()) != null) {
                out.println(line);
            }
        }
    }

    public static List<File> orderFiles(Collection<File> files) {
        List<File> result = new ArrayList<>(files);
        Collections.sort(result, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
            }
        });
        return result;
    }

    public static String sha1(Path in) throws IOException {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            return Hex.encodeHexString(md.digest(Files.readAllBytes(in)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toExtension(String mimeType, boolean throwDefault) {
        if (mimeType == null) {
            if (throwDefault) {
                throw new IllegalStateException("Unexpected null mime type");
            } else {
                return null;
            }
        }
        final String lowerMimeType = mimeType.toLowerCase(Locale.ROOT);
        if (lowerMimeType.contains("plain") || "text/x-c".equals(lowerMimeType)) {
            return ".txt";
        }

        if (lowerMimeType.contains("html")) {
            return ".html";
        }

        if (lowerMimeType.contains("pdf")) {
            return ".pdf";
        }

        if (throwDefault) {
            throw new IllegalStateException("Unexpected mime type '" + mimeType + "'");
        } else {
            return null;
        }
    }
}
