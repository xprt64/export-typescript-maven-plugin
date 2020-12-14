package com.github.xprt64.typescript;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.clapper.util.classutil.ClassFinder;
import org.clapper.util.classutil.ClassInfo;
import org.clapper.util.classutil.SubclassClassFilter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mojo(
    name = "dependency-counter",
    defaultPhase = LifecyclePhase.COMPILE,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class ExportPlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "commands")
    String[] commands;

    @Parameter(property = "questions")
    String[] questions;

    @Parameter(property = "apiDir")
    String apiDir;

    ClassLoader classLoader;
    ClassFinder finder;

    Consumer<Class<?>> newObjectReporter;

    ObjectConverter converter;
    private List<Class<?>> referencedObjects = new ArrayList<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        classLoader = makeClassLoader();
        finder = makeClassFinder();
        newObjectReporter = makeNewObjectReporter();
        converter = new ObjectConverter();

        Collection<ClassInfo> foundClasses = new ArrayList<>();
        getLog().info("searcing for " + commands.length + " commands");
        for (String command : commands) {
            getLog().info("searching for " + command);
            foundClasses.addAll(findClassesThatImplement(command));
        }
        getLog().info("found " + foundClasses.size() + " commands");
        for (ClassInfo classInfo : foundClasses) {
            exportCommand(classInfo);
        }

        Collection<ClassInfo> foundQuestions = new ArrayList<>();
        getLog().info("searcing for " + questions.length + " questions");
        for (String question : questions) {
            getLog().info("searching for " + question);
            foundQuestions.addAll(findClassesThatImplement(question));
        }
        getLog().info("found " + foundQuestions.size() + " questions");
        for (ClassInfo classInfo : foundQuestions) {
            exportQuestion(classInfo);
        }

        getLog().info("found " + referencedObjects.size() + " referenced objects");

        referencedObjects.forEach(clazz -> {
            exportReferencedObject(clazz);
        });
    }

    void exportClass(ClassInfo classInfo) {
        try {
            getLog().info("- " + classInfo.getClassName());
            Class clazz = classLoader.loadClass(classInfo.getClassName());
            getLog().info(converter.generateInterface(clazz, newObjectReporter).generateInterface());
            if (referencedObjects.size() > 0) {
                getLog().info("    referenced objects: " + String.join(", ", referencedObjects.stream().map(Class::toString).collect(Collectors.toList())));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    void exportCommand(ClassInfo classInfo) {
        try {
            getLog().info("- exporting command " + classInfo.getClassName());
            Class clazz = classLoader.loadClass(classInfo.getClassName());
            TypescriptInterface generatedInterface = converter.generateInterface(clazz, newObjectReporter);
            String code = TypescriptCommand.export(generatedInterface);
            getLog().info(code);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    void exportQuestion(ClassInfo classInfo) {
        try {
            getLog().info("- exporting question " + classInfo.getClassName());
            Class clazz = classLoader.loadClass(classInfo.getClassName());
            TypescriptInterface generatedInterface = converter.generateInterface(clazz, newObjectReporter);
            String code = TypescriptQuestion.export(generatedInterface);
            getLog().info(code);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    void exportReferencedObject(Class<?> clazz) {
        getLog().info("- " + clazz.getCanonicalName());
        getLog().info(converter.generateInterface(clazz, null).generateInterface());
    }



    Collection<ClassInfo> findClassesThatImplement(String interfaceName) {
        try {
            SubclassClassFilter filter = new SubclassClassFilter(classLoader.loadClass(interfaceName));
            Collection<ClassInfo> foundClasses = new ArrayList<ClassInfo>();
            finder.findClasses(foundClasses, filter);
            return foundClasses;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    ClassLoader makeClassLoader() throws MojoExecutionException {
        List runtimeClasspathElements = null;
        try {
            runtimeClasspathElements = project.getRuntimeClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage());
        }
        URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
        for (int i = 0; i < runtimeClasspathElements.size(); i++) {
            String element = (String) runtimeClasspathElements.get(i);
            try {
                runtimeUrls[i] = new File(element).toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new MojoExecutionException(e.getMessage());
            }
        }
        return new URLClassLoader(runtimeUrls,
            Thread.currentThread().getContextClassLoader());
    }

    ClassFinder makeClassFinder() {
        ClassFinder finder = new ClassFinder();
        finder.addClassPath();
        try {
            project.getCompileClasspathElements().forEach(s -> {
                getLog().info("adding file to class loader " + s);
                finder.add(new File(s));
            });
            project.getRuntimeClasspathElements().forEach(s -> {
                getLog().info("adding file to class loader " + s);
                finder.add(new File(s));
            });
        } catch (DependencyResolutionRequiredException e) {
            e.printStackTrace();
        }
        return finder;
    }

    Consumer<Class<?>> makeNewObjectReporter() {
        return (Class<?> clazz) -> referencedObjects.add(clazz);
    }
}
