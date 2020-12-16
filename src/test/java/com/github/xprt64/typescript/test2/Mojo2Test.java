package com.github.xprt64.typescript.test2;

import com.github.xprt64.typescript.ExportPlugin;
import com.github.xprt64.typescript.helpers.FileSystemTester;
import junit.framework.TestCase;

public class Mojo2Test extends TestCase {
    private FileSystemTester fileExporter;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        // required
        super.setUp();
        fileExporter = new FileSystemTester();
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
        sut.fileSystem = fileExporter;
        sut.classLoader = getClass().getClassLoader();
        sut.init();

        sut.exportReferencedObject(SimpleEnum.class);

        fileExporter.assertWrittenFilesCount(1);
        fileExporter.assertFileIsWritten("/test2/SimpleEnum.ts", "com/github/xprt64/typescript/test2/SimpleEnum.ts");

    }
}
