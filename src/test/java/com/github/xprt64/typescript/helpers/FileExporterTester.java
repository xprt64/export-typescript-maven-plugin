package com.github.xprt64.typescript.helpers;

import com.github.xprt64.typescript.FileExporter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class FileExporterTester extends FileExporter {

    private Map<String, String> files = new HashMap<>();

    public FileExporterTester() {
        super(null);
    }

    public void assertWrittenFilesCount(int count) {
        assertEquals(files.size(), count);
    }

    @Override
    public void writeFile(String path, String contents){
        files.put(path, contents);
    }

    public void assertFileIsWritten(String expectedResource, String actualWrittenFilePath) {
        assertTrue("expected file is not written: " + actualWrittenFilePath, fileIsWritten(actualWrittenFilePath));
        assertContentsEquals(readResource(expectedResource), getWrittenFileContents(actualWrittenFilePath));
    }

    private void assertContentsEquals(String expected, String actual){
        assertEquals(removeNoise(expected), removeNoise(actual));
    }

    private boolean fileIsWritten(String path){
        return files.containsKey(path);
    }

    private String getWrittenFileContents(String path){
        return files.get(path);
    }

    private String removeNoise(String contents){
        return contents.replaceAll("\\s+", " ");
    }

    public static String readResource(String path) {
        java.net.URL url = FileExporterTester.class.getResource(path);
        java.nio.file.Path resPath = null;
        try {
            resPath = java.nio.file.Paths.get(url.toURI());
            return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
        } catch (URISyntaxException | IOException e) {
            assertTrue(e.getMessage(), false);
            return null;
        }
    }
}
