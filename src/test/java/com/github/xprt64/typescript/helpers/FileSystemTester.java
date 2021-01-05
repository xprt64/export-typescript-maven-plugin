package com.github.xprt64.typescript.helpers;

import com.github.xprt64.typescript.FileSystem;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class FileSystemTester extends FileSystem {

    private Map<String, String> files = new HashMap<>();

    public FileSystemTester() {
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
//        System.out.println(actual);
        assertEquals(normalize(expected), normalize(actual));
//        assertEquals(removeNoise(expected), removeNoise(actual));
    }
    
    private String normalize(String input){
        return input.trim().replaceAll("\r\n", "\n");
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
        java.net.URL url = FileSystemTester.class.getResource(path);
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
