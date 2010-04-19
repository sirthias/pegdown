package org.pegdown;

import java.io.*;
import java.nio.charset.Charset;

public class FileUtils {

    private FileUtils() {}

    public static String readAllTextFromResource(String resource) {
        return readAllText(ClassLoader.getSystemClassLoader().getResourceAsStream(resource));
    }

    public static String readAllTextFromResource(String resource, Charset charset) {
        return readAllText(ClassLoader.getSystemClassLoader().getResourceAsStream(resource), charset);
    }

    public static String readAllText(String filename) {
        return readAllText(new File(filename));
    }

    public static String readAllText(String filename, Charset charset) {
        return readAllText(new File(filename), charset);
    }

    public static String readAllText(File file) {
        return readAllText(file, Charset.forName("UTF8"));
    }

    public static String readAllText(File file, Charset charset) {
        try {
            return readAllText(new FileInputStream(file), charset);
        }
        catch (FileNotFoundException e) {
            return null;
        }
    }

    public static String readAllText(InputStream stream) {
        return readAllText(stream, Charset.forName("UTF8"));
    }

    public static String readAllText(InputStream stream, Charset charset) {
        if (stream == null) return null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
        StringWriter writer = new StringWriter();
        copyAll(reader, writer);
        return writer.toString();
    }

    public static byte[] readAllBytesFromResource(String resource) {
        return readAllBytes(ClassLoader.getSystemClassLoader().getResourceAsStream(resource));
    }

    public static byte[] readAllBytes(String filename) {
        return readAllBytes(new File(filename));
    }

    public static byte[] readAllBytes(File file) {
        try {
            return readAllBytes(new FileInputStream(file));
        }
        catch (FileNotFoundException e) {
            return null;
        }
    }

    public static byte[] readAllBytes(InputStream stream) {
        if (stream == null) return null;
        BufferedInputStream in = new BufferedInputStream(stream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copyAll(in, out);
        return out.toByteArray();
    }

    public static void writeAllText(String text, String filename) {
        writeAllText(text, new File(filename));
    }

    public static void writeAllText(String text, String filename, Charset charset) {
        writeAllText(text, new File(filename), charset);
    }

    public static void writeAllText(String text, File file) {
        try {
            ensureParentDir(file);
            writeAllText(text, new FileOutputStream(file), Charset.forName("UTF8"));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeAllText(String text, File file, Charset charset) {
        try {
            writeAllText(text, new FileOutputStream(file), charset);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeAllText(String text, OutputStream stream) {
        writeAllText(text, stream, Charset.forName("UTF8"));
    }

    public static void writeAllText(String text, OutputStream stream, Charset charset) {
        StringReader reader = new StringReader(text != null ? text : "");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, charset));
        copyAll(reader, writer);
    }

    public static void writeAllBytes(byte[] data, String filename) {
        writeAllBytes(data, new File(filename));
    }

    public static void writeAllBytes(byte[] data, File file) {
        try {
            ensureParentDir(file);
            writeAllBytes(data, new FileOutputStream(file));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeAllBytes(byte[] data, OutputStream stream) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        BufferedOutputStream out = new BufferedOutputStream(stream);
        copyAll(in, out);
    }

    public static void copyAll(Reader reader, Writer writer) {
        try {
            char[] data = new char[4096]; // copy in chunks of 4K
            int count;
            while ((count = reader.read(data)) >= 0) writer.write(data, 0, count);

            reader.close();
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyAll(InputStream in, OutputStream out) {
        try {
            byte[] data = new byte[4096]; // copy in chunks of 4K
            int count;
            while ((count = in.read(data)) >= 0) {
                out.write(data, 0, count);
            }

            in.close();
            out.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void ensureParentDir(String filename) {
        ensureParentDir(new File(filename));
    }

    public static void ensureParentDir(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            try {
                forceMkdir(parentDir);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Could not create directory %s", parentDir), e);
            }
        }
    }

    public static void forceMkdir(File directory) throws IOException {
        if (directory.exists()) {
            if (directory.isFile()) {
                throw new IOException(
                        "File '" + directory + "' exists and is not a directory. Unable to create directory.");
            }
        } else {
            if (!directory.mkdirs()) {
                throw new IOException("Unable to create directory " + directory);
            }
        }
    }

    public static String createTempFileName(String prefix) {
        return createTempFileName(prefix, null);
    }

    private static String createTempFileName(String prefix, String suffix) {
        try {
            return File.createTempFile(prefix, suffix).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp file", e);
        }
    }

}

