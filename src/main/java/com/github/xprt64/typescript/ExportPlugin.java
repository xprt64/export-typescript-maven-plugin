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
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mojo(
    name = "dependency-counter",
    defaultPhase = LifecyclePhase.COMPILE,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class ExportPlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "subClassesOf")
    String[] subClassesOf;

    ClassLoader classLoader;
    ClassFinder finder;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        classLoader = makeClassLoader();
        finder = makeClassFinder();

        Collection<ClassInfo> foundClasses = new ArrayList<>();
        getLog().info("searcing for " + subClassesOf.length + " interfaces/sub classes");

        for(String interfaceName : subClassesOf){
            getLog().info("searching for " + interfaceName);
            foundClasses.addAll(findClassesThatImplement(interfaceName));
        }
        getLog().info("found " + foundClasses.size() + " classes");

        for (ClassInfo classInfo : foundClasses) {

            System.out.println("- " + classInfo.getClassName());
            exportClass(classInfo);
            // consider also using classInfo.getClassLocation() to get the
            // jar file providing it
        }
    }

    void exportClass(ClassInfo classInfo){
        try {
            getLog().info( classInfo.getClassName() );
            Class clazz = classLoader.loadClass(classInfo.getClassName());
             for( Field field: clazz.getDeclaredFields()){
                field.setAccessible(true);

                getLog().info( "    " + field.getName() + ":" + field.getType().getCanonicalName() + ":" + (field.getType().isPrimitive() ? " primitive" : " object") + ":" + (field.isSynthetic() ? " sintetic" : "") );
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
}
