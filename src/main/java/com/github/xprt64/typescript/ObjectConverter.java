package com.github.xprt64.typescript;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class ObjectConverter {

    public TypescriptInterface generateInterface(Class<?> clazz, Consumer<Class<?>> newObjectReporter) {
        List<String> res = new ArrayList<>();
        List<Class<?>> referencedClasses = new ArrayList<>();
        Consumer<Class<?>> reporter = (referencedClazz) -> {
            if (newObjectReporter != null) {
                newObjectReporter.accept(referencedClazz);
            }
            referencedClasses.add(referencedClazz);
        };
        System.out.println("\n\n\n");
        System.out.println("converting " + clazz.getCanonicalName());
        Type genericSuperclassc = clazz.getGenericSuperclass();
        if (null != genericSuperclassc && !genericSuperclassc.getTypeName().equals("java.lang.Object")) {
            reporter.accept(clazz.getSuperclass());
//            if (genericSuperclassc instanceof ParameterizedType) {
//                System.out.println("found generic super class type name " + ((ParameterizedType) genericSuperclassc).getTypeName());
//                Type[] genericTypes = ((ParameterizedType) genericSuperclassc).getActualTypeArguments();
//                for (Type genericType : genericTypes) {
//                    System.out.println("    Generic type class: " + genericType.getClass().getCanonicalName());
//                    System.out.println("    Generic type: " + genericType.getTypeName());
//                }
//            }

        }
        Arrays.stream(clazz.getInterfaces()).forEach(inter -> {
            reporter.accept(inter);
        });
        addMembers(clazz, res, reporter);
        return new TypescriptInterface(clazz, referencedClasses, String.join("", res));
    }

    private void addMembers(Class<?> clazz, List<String> output, Consumer<Class<?>> reporter) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            System.out.println("found item " + field.getName() + ":" + field.getType().getCanonicalName() + ":" + field.getGenericType().getTypeName());
            String converted = null;
            if (isArray(field)) {
                converted = convertArray(field, reporter);
            } else if (isList(field)) {
                converted = convertList(field, reporter);
            } else {
                converted = tryConvertPrimitive(field);
                if (converted == null) {
                    converted = convertAsObject(field, reporter);
                }

            }
            output.add("     " + converted + ";\n");
        }
    }

    private String tryConvertPrimitive(Field field) {
        if (isBasic(field)) {
            String basicType = getPrimitiveType(field);
            return field.getName() + (isOptional(field) ? "?" : "") + ":" + basicType;
        } else {
            return null;
        }
    }

    private String convertList(Field field, Consumer<Class<?>> newObjectReporter) {
        String typescriptType = "any[]";
        if (field.getGenericType() instanceof ParameterizedType) {
            Type t = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (t instanceof Class) {
                Class c = (Class) t;
                typescriptType = resolveToTypescript(c, newObjectReporter);
            }
        }

        newObjectReporter.accept(field.getType());
        return field.getName() + (isOptional(field) ? "?" : "") + ":" + typescriptType + "[]";
    }

    private String resolveToTypescript(Class c, Consumer<Class<?>> newObjectReporter) {
        String type = null;
        if (isBasic(c)) {
            type = getPrimitiveType(c);
        } else {
            newObjectReporter.accept(c);
            type = c.getSimpleName();
        }
        return type + (c.isArray() ? "[]" : "");
    }


    private String convertArray(Field field, Consumer<Class<?>> newObjectReporter) {
        String typescriptType = "any";
        Class c = field.getType().getComponentType();
        if (field.getGenericType() instanceof GenericArrayType) {
            Type t1 = ((GenericArrayType) field.getGenericType()).getGenericComponentType();
            if (t1 instanceof ParameterizedType) {
                Type t = ((ParameterizedType) t1).getActualTypeArguments()[0];
                if (t instanceof Class) {
                    Class c1 = (Class) t;
                    typescriptType = resolveToTypescript(c1, newObjectReporter) + "[]";
                }
            }
        } else {
            typescriptType = resolveToTypescript(c, newObjectReporter);
        }
        return field.getName() + (isOptional(field) ? "?" : "") + ":" + typescriptType + "[]";
    }

    private boolean isList(Field field) {
        return List.class.isAssignableFrom(field.getType());
    }

    private boolean isArray(Field field) {
        return field.getType().isArray();
    }

    private boolean isBasic(Field field) {
        return isBasic(field.getType());
    }

    private boolean isBasic(Class<?> clazz) {
        if (clazz.isArray()) {
            return isBasic(clazz.getComponentType());
        }
        return clazz.isPrimitive() || clazz.isAssignableFrom(String.class)
            || clazz.isAssignableFrom(Integer.class)
            || clazz.isAssignableFrom(Date.class)
            || clazz.isAssignableFrom(Float.class)
            || clazz.isAssignableFrom(Boolean.class)
            || clazz.isAssignableFrom(Double.class)
            ;
    }

    private boolean isOptional(Field field) {
        return !Modifier.isFinal(field.getModifiers());
    }

    private String getPrimitiveType(Field field) {
        return getPrimitiveType(field.getType());
    }

    private String getPrimitiveType(Class clazz) {
        switch (clazz.getSimpleName().toLowerCase().replaceAll("(\\[|\\])*$", "")) {
            case "int":
            case "integer":
            case "long":
                return "bigint";
            case "double":
            case "float":
                return "number";
            case "string":
            case "byte":
            case "date":
                return "string";
            case "boolean":
                return "boolean";
//            case "object":
//                return "any";
            default:
                throw new RuntimeException("tip primitiv necunoscut:" + clazz.getSimpleName());
        }
    }

    private String convertAsObject(Field field, Consumer<Class<?>> newObjectReporter) {
        newObjectReporter.accept(field.getType());
        return field.getName() + (isOptional(field) ? "?" : "") + ":" + field.getType().getCanonicalName();
    }

}
