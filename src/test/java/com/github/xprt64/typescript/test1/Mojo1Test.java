package com.github.xprt64.typescript.test1;

import com.github.xprt64.typescript.ExportPlugin;
import com.github.xprt64.typescript.helpers.FileExporterTester;
import junit.framework.TestCase;

public class Mojo1Test extends TestCase {
    private FileExporterTester fileExporter;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        // required
        super.setUp();
        fileExporter = new FileExporterTester();
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        // required
        super.tearDown();
    }

    public void testSomething() throws Exception {
        ExportPlugin sut = new ExportPlugin();
        sut.fileExporter = fileExporter;
        sut.classLoader = getClass().getClassLoader();
        sut.init();

        sut.exportCommand(Command1.class);

        fileExporter.assertWrittenFilesCount(1);
        fileExporter.assertFileIsWritten("/test1/Command1.ts", "com/github/xprt64/typescript/test1/Command1.ts");

    }
}