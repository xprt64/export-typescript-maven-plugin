package com.github.xprt64.typescript;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class FileWriter {
    static void writeToFile(TypescriptInterface typescriptInterface, String code, String apiDir) {
        String directoryName = apiDir + "/" + typescriptInterface.getRelativeDirPath();

        File directory = new File(directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Could not create directory " + directoryName);
            }
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }

        File file = new File(directoryName + "/" + typescriptInterface.getFileName() + ".ts");
        try {
            java.io.FileWriter fw = new java.io.FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(code);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
