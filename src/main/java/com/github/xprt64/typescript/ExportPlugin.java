package com.github.xprt64.typescript;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.DefaultPluginDescriptorCache;
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

import javax.lang.model.type.ArrayType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mojo(
    name = "generate-api",
    defaultPhase = LifecyclePhase.COMPILE,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class ExportPlugin extends AbstractMojo {

    public static final String INDENT = "    ";
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "commands")
    String[] commands;

    @Parameter(property = "questions")
    String[] questions;

    @Parameter(property = "apiDir", defaultValue = "api")
    String apiDir;

    public ClassLoader classLoader;
    ClassFinder finder;

    private Map<String, Class<?>> emitedObjects = new HashMap<>();
    private Map<String, Class<?>> referencedObjects = new HashMap<>();

    public FileSystem fileSystem;
    private int indent = 0;
    private Set<String> exportedObjects = new HashSet<>();;

    public ExportPlugin() {

    }

    public void init() throws MojoExecutionException {
        if (null == fileSystem) {
            fileSystem = new FileSystem(this.apiDir);
        }
        if (null == classLoader) {
            classLoader = makeClassLoader();
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            init();
            finder = makeClassFinder();

            getLog().info("searcing for " + commands.length + " commands");
            Arrays.stream(commands).forEach(
                className -> findClassesThatImplement(className).forEach(this::exportCommand));

            getLog().info("searching for " + questions.length + " questions");
            Arrays.stream(questions).forEach(
                className -> findClassesThatImplement(className).forEach(this::exportQuestion));

            getLog().info("found " + emitedObjects.size() + " referenced objects");

            do{
                Collection<Class<?>> values = emitedObjects.values().stream().collect(Collectors.toList());
                emitedObjects.clear();
                values.forEach(this::exportReferencedObject);

            }
            while(emitedObjects.size() > 0);

            writeApiDelegate();
        } catch (Throwable e) {
            e.printStackTrace();
            getLog().error(e.getMessage());
            getLog().error(e.getClass().getCanonicalName());
            getLog().error(e.getCause().getMessage());
            throw e;
        }

    }

    public void exportCommand(Class<?> clazz) {
        getLog().info("- exporting command " + clazz.getCanonicalName());
        exportReferencedObject(
            clazz,
            imports -> {
                String path = String.join("/", FileSystem.relative(clazzComponents(clazz), pathComponents("api_delegate")));
                return imports + "import {dispatchCommand} from '" + path + "';\n";
            },
            body -> body + "\n" + "export async function dispatch" + clazz.getSimpleName() + "(comanda: " + clazz.getSimpleName() + ") {\n" +
                "    return await dispatch(comanda)\n" +
                "}\n" +
                "\n" +
                "export async function dispatch(comanda: " + clazz.getSimpleName() + ") {\n" +
                "    return await dispatchCommand(\"" + clazz.getCanonicalName() + "\", comanda)\n" +
                "}\n"
        );
    }

    String getOutputDir() {
        return project.getBasedir() + "/" + apiDir;
    }

    void exportQuestion(Class<?> clazz) {
        getLog().info("- exporting question " + clazz.getCanonicalName());
        exportReferencedObject(
            clazz,
            imports -> {
                String path = String.join("/", FileSystem.relative(clazzComponents(clazz), pathComponents("api_delegate")));
                return imports + "import {answerQuestion} from '" + path + "';\n";
            },
            body -> body + "export async function askQuestion" + clazz.getSimpleName() + "(question: " + clazz.getSimpleName() + "): Promise<" + clazz.getSimpleName() + "> {\n" +
                "    return await askQuestion(question)\n" +
                "}\n" +
                "\n" +
                "export async function askQuestion(question: " + clazz.getSimpleName() + "): Promise<" + clazz.getSimpleName() + "> {\n" +
                "    return await answerQuestion(\"" + clazz.getCanonicalName() + "\", question)\n" +
                "}"
        );
    }

    public void exportReferencedObject(Class<?> clazz) {
        exportReferencedObject(clazz, null, null);
    }

    public void exportEnum(Class<?> clazz) {
        String result = "export enum " + clazz.getSimpleName() + " {" + "\n"
             + String.join("\n", getEnumValues(clazz) .stream().map( e -> INDENT + e + " = \"" + e + "\"," ).collect(Collectors.toList())) + "\n}";
        fileSystem.writeFile(componentsToPath(clazzComponents(clazz)) + ".ts", result);
    }

    private List<String> getEnumValues(Class<?> clazz) {
        return Arrays.stream(clazz.getEnumConstants()).map(s -> s.toString()).collect(Collectors.toList());
    }

    public void exportReferencedObject(Class<?> clazz, Function<String, String> importsParser, Function<String, String> bodyParser) {
        if(exportedObjects.contains(clazz.getCanonicalName())){
            return;
        }
        exportedObjects.add(clazz.getCanonicalName());

        getLog().info("- " + clazz.getCanonicalName());

        referencedObjects.clear();

        if(clazz.isEnum()){
            exportEnum(clazz);
            return;
        }

        StringBuilder body = new StringBuilder();
        body.append("export interface ")
            .append(clazz.getSimpleName());

        List<String> params = Arrays.stream(clazz.getTypeParameters()).map(p -> p.getName()).collect(Collectors.toList());
        Predicate<String> isParameter = c -> !c.contains(".") && params.stream().filter(p -> p.equals(c)).count() > 0;

        if (clazz.getTypeParameters().length > 0) {
            body.append("<" + String.join(", ", params) + ">");
        }
        /**
         * The parameterized type representing the superclass is created if it had not been created before.
         * See the declaration of ParameterizedType for the semantics of the creation process for parameterized types.
         * If this Class object represents either the Object class, an interface, a primitive type, or void, then null is returned.
         * If this Class object represents an array class then the Class object representing the Object class is returned
         */

        List<String> extendList = new ArrayList<>();

        Type genericSuperClassType = clazz.getGenericSuperclass();
        if (null == genericSuperClassType || genericSuperClassType.equals(Object.class)) {
            // Object class, an interface, a primitive type, or void
        }
        else {
            extendList.add(javaTypeToTsType(genericSuperClassType, isParameter));
        }
        Arrays.stream(clazz.getGenericInterfaces()).forEach(type -> {
            echo("extended interface: " + type.getTypeName());
            extendList.add(javaTypeToTsType(type, isParameter));
        });
        List<String> filteredExtendList = extendList.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (filteredExtendList.size() > 0) {
            body.append(" extends " + String.join(", ", filteredExtendList));
        }
        body.append("\n{\n");

        StringBuilder fields = new StringBuilder();
        Arrays.stream(clazz.getDeclaredFields()).forEach(field -> {
            field.setAccessible(true);
            String typeString = "";
            String append = "";
            if (field.getType().isArray()) {
                if (field.getGenericType() instanceof GenericArrayType) {
                    GenericArrayType arrT = (GenericArrayType) field.getGenericType();
                    typeString = javaTypeToTsType(arrT.getGenericComponentType(), isParameter);
                } else {
                    Class<?> ct = field.getType().getComponentType();
                    while (ct.isArray()) {
                        append += "[]";
                        ct = ct.getComponentType();
                    }

                    typeString = ct.isPrimitive() ? javaPrimitiveToTsPrimitive(ct) : javaTypeToTsType(ct, isParameter);
                }
                typeString += "[]";

            } else {
                typeString = field.getType().isPrimitive() ? javaPrimitiveToTsPrimitive(field.getType()) : javaTypeToTsType(field.getGenericType(), isParameter);
            }

            String optionalString = Modifier.isFinal(field.getModifiers()) ? "" : "?";
            fields.append(INDENT + field.getName() + optionalString + ": " + typeString + append + ";\n");
        });
        body.append(fields.toString());

        body.append("}\n");

        StringBuilder imports = new StringBuilder();

        referencedObjects.values().stream().filter(c -> !c.getCanonicalName().equals(clazz.getCanonicalName())).forEach(ref -> {
            String path = String.join("/", FileSystem.relative(clazzComponents(clazz), clazzComponents(ref)));
            imports.append("import {" + ref.getSimpleName() + "} from '" + path + "';\n");
        });

        String importsString = importsParser == null ? imports.toString() : importsParser.apply(imports.toString());
        String result = (importsString.equals("") ? "" : importsString + "\n") +
            (bodyParser == null ? body.toString() : bodyParser.apply(body.toString()));

        fileSystem.writeFile(componentsToPath(clazzComponents(clazz)) + ".ts", result);
    }

    private String javaPrimitiveToTsPrimitive(Class<?> type) {
        return getPrimitiveType(type);
    }

    void echo(String str) {
        String textIndent = new String(new char[indent * 4]).replace("\0", " ");
        //System.out.println(textIndent + str);
    }

    String javaTypeToTsType(Type type, Predicate<String> isParameter) {
        indent++;
        echo("-" + type);
        if (isParameter.test(type.getTypeName())) {
            echo(" type is parameter");
            return type.getTypeName();
        }


        if (type instanceof GenericArrayType) {
            echo(" type is GenericArrayType");
            echo(" type parameters:");
            ParameterizedType parametrizedType = (ParameterizedType) type;
            List<String> parameters = Arrays.stream(parametrizedType.getActualTypeArguments()).map(a -> javaTypeToTsType(a, isParameter)).collect(Collectors.toList());
            indent--;
            if (Collection.class.isAssignableFrom(loadClass(parametrizedType.getRawType().getTypeName()))) {
                return parameters.get(0) + "[]";
            }
            return emitClass(parametrizedType.getRawType().getTypeName()) + "<" + String.join(", ", parameters) + ">";
        }
        if (type instanceof ParameterizedType) {
            echo(" type is generic");
            echo(" type parameters:");
            ParameterizedType parametrizedType = (ParameterizedType) type;
            List<String> parameters = Arrays.stream(parametrizedType.getActualTypeArguments()).map(a -> javaTypeToTsType(a, isParameter)).collect(Collectors.toList());
            indent--;
            if (Collection.class.isAssignableFrom(loadClass(parametrizedType.getRawType().getTypeName()))) {
                return parameters.get(0) + "[]";
            }
            return emitClass(parametrizedType.getRawType().getTypeName()) + "<" + String.join(", ", parameters) + ">";
        }
        echo(" type is NOT generic");
        String typeName = type.getTypeName();
        String append = "";
        while (typeName.endsWith("[]")) {
            typeName = typeName.substring(0, typeName.length() - 2);
            append += "[]";
        }
        echo(" typeName: " + typeName);
        indent--;
        if (typeName.contains(".")) {
            String emittedClassName = emitClass(typeName);
            return emittedClassName == null ? null : (emittedClassName + append);
        }
        return getPrimitiveType(typeName) + append;
    }

    String emitClass(String clazz) {
        return emitClass(loadClass(clazz));
    }

    String emitClass(Class<?> clazz) {
        if (clazz.equals(Object.class)) {
            return "any";
        }
        if (isIgnored(clazz)) {
            return null;
        }
        if (isBasic(clazz)) {
            return getPrimitiveType(clazz);
        }
        echo("! class emitted: " + clazz.getCanonicalName());
        emitedObjects.put(clazz.getCanonicalName(), clazz);
        referencedObjects.put(clazz.getCanonicalName(), clazz);

        return clazz.getSimpleName();
    }

    private boolean isIgnored(Class<?> clazz) {
        if (clazz.isInterface()) {
            if (clazz.equals(Serializable.class)) {
                return true;
            }
            if (clazz.equals(Cloneable.class)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBasic(Class<?> clazz) {
        if (clazz.isArray()) {
            return isBasic(clazz.getComponentType());
        }
        return clazz.isPrimitive()
            || clazz.isAssignableFrom(String.class)
            || clazz.isAssignableFrom(Integer.class)
            || clazz.isAssignableFrom(Date.class)
            || clazz.isAssignableFrom(Float.class)
            || clazz.isAssignableFrom(Boolean.class)
            || clazz.isAssignableFrom(Double.class)
            ;
    }

    private String getPrimitiveType(Class<?> clazz) {
        return getPrimitiveType(clazz.getSimpleName());
    }

    private String getPrimitiveType(String name) {
        switch (name.toLowerCase().replaceAll("(\\[|\\])*$", "")) {
            case "long":
                return "bigint";
            case "int":
            case "integer":
            case "double":
            case "float":
                return "number";
            case "string":
            case "byte":
            case "date":
                return "string";
            case "boolean":
                return "boolean";
            case "object":
                return "any";
            default:
                throw new RuntimeException("tip primitiv necunoscut:" + name);
        }
    }

    private String componentsToPath(LinkedList<String> c) {
        return String.join("/", c);
    }

    private LinkedList<String> clazzComponents(Class clazz) {
        return new LinkedList<>(Arrays.asList(clazz.getCanonicalName().split(Pattern.quote("."))));
    }

    private LinkedList<String> pathComponents(String path) {
        return new LinkedList<>(Arrays.asList(path.split(Pattern.quote("/"))));
    }


    Collection<Class<?>> findClassesThatImplement(String interfaceName) {
        try {
            SubclassClassFilter filter = new SubclassClassFilter(classLoader.loadClass(interfaceName));
            Collection<ClassInfo> foundClasses = new ArrayList<ClassInfo>();
            finder.findClasses(foundClasses, filter);
            return foundClasses.stream().map(classInfo -> loadClass(classInfo.getClassName())).collect(
                Collectors.toList());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            getLog().error("error loading class: `" + className + "`; error is:" + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
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
                finder.add(new File(s));
            });
            project.getRuntimeClasspathElements().forEach(s -> {
                finder.add(new File(s));
            });
        } catch (DependencyResolutionRequiredException e) {
            e.printStackTrace();
        }
        return finder;
    }

//    Function<Class<?>, ExportedType> emitClass() {
//        return (Class<?> clazz) -> {
//            String ret = tryToMapToBasicType(clazz);
//            if(ret == null)
//            {
//                referencedObjects.put(clazz.getCanonicalName(), clazz);
//                return clazz.getSimpleName()
//            }
//        }
//    }

    void writeApiDelegate() {
        String filename = "api_delegate.ts";
        InputStream in = getClass().getResourceAsStream("/" + filename);

        try {

            Files.copy(in, Paths.get(getOutputDir(), filename));
        } catch (FileAlreadyExistsException e) {
            getLog().info("file not overwritten, already exists: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
