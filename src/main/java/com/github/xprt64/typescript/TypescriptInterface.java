package com.github.xprt64.typescript;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TypescriptInterface {
    private Class<?> clazz;
    private List<Class<?>> imports;
    private List<String[]> otherImports = new ArrayList<>();
    private String body;
    private Function<Type, String> typeResolver;

    public TypescriptInterface(Class<?> clazz, List<Class<?>> referencedClasses, String body, Function<Type, String> typeResolver) {
        this.clazz = clazz;
        this.imports = referencedClasses;
        this.body = body;
        this.typeResolver = typeResolver;
    }

    private List<String> getEnumValues(Class<?> clazz) {
        return Arrays.stream(clazz.getEnumConstants()).map(s -> s.toString()).collect(Collectors.toList());
    }

    private String generateEnum(){
        return "export enum " + clazz.getSimpleName() + " { " + "\n" + String.join("\n", getEnumValues(clazz) .stream().map( e -> e + " = \"" + e + "\", " ).collect(Collectors.toList())) + " }";

    }

    public String generateInterface() {
        if (clazz.isEnum()) {
            return generateEnum();
        }
        String name = clazz.getSimpleName();
        List<String> implementations = new ArrayList<>();
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (null != genericSuperclass && !genericSuperclass.getTypeName().equals("java.lang.Object")) {
            String superClassSignature = clazz.getGenericSuperclass().getTypeName();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericSuperclass;

                Arrays.stream(pt.getActualTypeArguments()).forEach(t -> {
                    System.out.println(" - " + t.getTypeName());
                });
                if (pt.getRawType() instanceof Class) {
                    Class<?> c1 = (Class<?>) pt.getRawType();
                    superClassSignature = c1.getSimpleName() + "<" + String.join(", ", Arrays.stream(pt.getActualTypeArguments()).map(t -> typeResolver.apply(t)).collect(Collectors.toList())) + ">";
                }
            }
            implementations.add(superClassSignature);
        }
        if (clazz.getInterfaces().length > 0) {
            implementations.addAll(Arrays.stream(clazz.getInterfaces())
                .filter(ObjectConverter::shouldIncludeInterfaceOrClass)
                .map(inter -> inter.getSimpleName()).collect(Collectors.toList()));
        }

        if (clazz.getTypeParameters().length > 0) {
                name = name + "<" + String.join(", ", Arrays.stream(clazz.getTypeParameters()).map(t -> typeResolver.apply(t)).collect(Collectors.toList())) + ">";
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

        return result.stream().distinct().collect(Collectors.toList());
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
        for (int i = 0; i < base.size() - 1; i++) {
            reference.addFirst("..");
        }
        if (!reference.getFirst().equals("..")) {
            reference.addFirst(".");
        }
        return reference;
    }

    public void addImport(String what, String fromPath) {
        otherImports.add(new String[]{what, fromPath});
    }

    public String getSimpleName() {
        return clazz.getSimpleName();
    }

    public String getSimpleNameFromString(String str) {
        String[] split = str.split(Pattern.quote("."));
        return split[split.length - 1];
    }

    public String getCanonicalName() {
        return clazz.getCanonicalName();
    }
}
