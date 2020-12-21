package com.github.xprt64.typescript.test3;

import com.github.xprt64.typescript.ExportPlugin;
import com.github.xprt64.typescript.helpers.FileSystemTester;
import junit.framework.TestCase;

public class Mojo1Test extends TestCase {
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

        sut.exportCommand(Command1.class);

        fileExporter.assertWrittenFilesCount(0);

    }
}
