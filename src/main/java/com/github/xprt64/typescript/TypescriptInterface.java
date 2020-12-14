package com.github.xprt64.typescript;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypescriptInterface {
    private Class<?> clazz;
    private List<Class<?>> imports;
    private List<String[]> otherImports = new ArrayList<>();
    private String body;

    public TypescriptInterface(Class<?> clazz, List<Class<?>> referencedClasses, String body) {
        this.clazz = clazz;
        this.imports = referencedClasses;
        this.body = body;
    }

    private List<String> getEnumValues(Class<?> clazz) {
        return Arrays.stream(clazz.getEnumConstants()).map(s -> "\"" + s.toString() + "\"").collect(Collectors.toList());
    }

    public String generateInterface() {
        if (clazz.isEnum()) {
            return "export type " + clazz.getSimpleName() + " = " + String.join("|", getEnumValues(clazz)) + ";";
        }
        String name = clazz.getSimpleName();
        List<String> implementations = new ArrayList<>();
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (null != genericSuperclass && !genericSuperclass.getTypeName().equals("java.lang.Object")) {
            implementations.add(clazz.getGenericSuperclass().getTypeName());
        }
        if (clazz.getInterfaces().length > 0) {
            implementations.addAll(Arrays.stream(clazz.getInterfaces()).map(inter -> inter.getSimpleName()).collect(Collectors.toList()));
        }
        return "export interface " + name + "" + (implementations.size() > 0 ? " extends " + String.join(", ", implementations) : "") + " {\n" +
                "" + body +
                "}";
    }

    public String getRelativeDirPath() {
        LinkedList<String> strings = clazzComponents(clazz);
        strings.removeLast();
        return String.join("/", strings);
    }

    public String getFileName() {
        return clazzComponents(clazz).getLast();
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public List<Class<?>> getImports() {
        return imports;
    }

    public List<String> generateImports() {
        List<String> result = imports
                .stream()
                .map(importedClass -> {
                    String path = String.join("/", relative(clazzComponents(clazz), clazzComponents(importedClass)));
                    return "import {" + importedClass.getSimpleName() + "} from '" + path + "';";
                })
                .collect(Collectors.toList());

        result.addAll(
                otherImports
                        .stream()
                        .map(other -> {
                            String path = String.join("/", relative(clazzComponents(clazz), pathComponents(other[1])));
                            return "import {" + other[0] + "} from '" + path + "';";

                        })
                        .collect(Collectors.toList())
        );

        return result;
    }

    private LinkedList<String> clazzComponents(Class clazz) {
        return new LinkedList<>(Arrays.asList(clazz.getCanonicalName().split(Pattern.quote("."))));
    }

    private LinkedList<String> pathComponents(String path) {
        return new LinkedList<>(Arrays.asList(path.split(Pattern.quote("/"))));
    }

    private static LinkedList<String> relative(LinkedList<String> base, LinkedList<String> reference) {
        while (base.size() > 0 && reference.size() > 0) {
            String b = base.get(0);
            String r = reference.get(0);
            if (b.equals(r)) {
                base.removeFirst();
                reference.removeFirst();
            } else {
                break;
            }
        }
        for (int i = 0; i < base.size()-1; i++) {
            reference.addFirst("..");
        }
        return reference;
    }

    public void addImport(String what, String fromPath) {
        otherImports.add(new String[]{what, fromPath});
    }

    public String getSimpleName() {
        return clazz.getSimpleName();
    }

    public String getCanonicalName() {
        return clazz.getCanonicalName();
    }
}
