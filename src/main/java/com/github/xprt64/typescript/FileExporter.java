package com.github.xprt64.typescript;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Pattern;

public class FileExporter {

    private final String basePath;

    public FileExporter(String basePath) {
        this.basePath = basePath;
    }

    private ParsedPath parsePath(String path) {
        LinkedList<String> pathComponents = pathComponents(path);
        String filename = pathComponents.removeLast();
        String dirname = String.join("/", pathComponents);
        return new ParsedPath(dirname, filename);
    }

    private LinkedList<String> pathComponents(String path) {
        return new LinkedList<>(Arrays.asList(path.split(Pattern.quote("/"))));
    }

    public void writeFile(String path, String contents) {
        ParsedPath parsedPath = parsePath(path);
        File directory = new File(parsedPath.dirname);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Could not create directory " + parsedPath.dirname);
            }
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }

        File file = new File(parsedPath.dirname + "/" + parsedPath.filename);
        try {
            java.io.FileWriter fw = new java.io.FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(contents);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    static class ParsedPath {
        public final String dirname;
        public final String filename;

        public ParsedPath(String dirname, String filename) {
            this.dirname = dirname;
            this.filename = filename;
        }
    }
}
