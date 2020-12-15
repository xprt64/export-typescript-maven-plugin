package com.github.xprt64.typescript;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ObjectConverter {

    public TypescriptInterface generateInterface(Class<?> clazz, Consumer<Class<?>> newObjectReporter, ClassLoader classLoader) {
        List<String> res = new ArrayList<>();
        List<Class<?>> referencedClasses = new ArrayList<>();
        Consumer<Class<?>> reporter = (referencedClazz) -> {
            if (newObjectReporter != null) {
                newObjectReporter.accept(referencedClazz);
            }
            referencedClasses.add(referencedClazz);
        };
        if (!clazz.isEnum()) {
            Type genericSuperclass = clazz.getGenericSuperclass();
            if (null != genericSuperclass && !genericSuperclass.getTypeName().equals("java.lang.Object")) {
                reporter.accept(clazz.getSuperclass());
                if (genericSuperclass instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) genericSuperclass;
                    Arrays.stream(pt.getActualTypeArguments()).forEach(t -> {
                        try {
                            Class<?> typeClass = classLoader.loadClass(t.getTypeName());
                            if (!isBasic(typeClass) && !typeClass.getCanonicalName().equals(clazz.getCanonicalName())) {
                                reporter.accept(typeClass);
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
            Arrays.stream(clazz.getInterfaces()).forEach(inter -> {
                if (!shouldIncludeInterfaceOrClass(inter)) {
                    return;
                }
                reporter.accept(inter);
            });
            addMembers(clazz, res, reporter);
        }

        return new TypescriptInterface(clazz, referencedClasses, String.join("", res), factoryTypeResolver());
    }

    private Function<Type, String> factoryTypeResolver() {
        return (type) -> {
            if (type instanceof Class) {
                Class<?> c = (Class<?>) type;
                if (isBasic(c)) {
                    return getPrimitiveType(c);
                }
                return (c).getSimpleName();
            } else {
                return type.getTypeName();
            }
        };
    }

    private void addMembers(Class<?> clazz, List<String> output, Consumer<Class<?>> reporter) {
        Function<Field, Boolean> fieldIsTemplateParameter = (Field field) -> {
            return Arrays.stream(clazz.getTypeParameters()).filter(t -> t.getName().equals(field.getGenericType().getTypeName())).count() > 0;
        };

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            //System.out.println("found item " + field.getName() + ":" + field.getType().getCanonicalName() + ":" + field.getGenericType().getTypeName());
            String converted = null;
            if (fieldIsTemplateParameter.apply(field)) {
                converted = convertGenericField(field, reporter);
            } else if (isArray(field)) {
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

    private String convertGenericField(Field field, Consumer<Class<?>> reporter) {
        return field.getName() + (isOptional(field) ? "?" : "") + ":" + field.getGenericType().getTypeName() + (field.getType().isEnum() ? "[]" : "");
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
        Type t = field.getGenericType();
        if (field.getName().equals("answer")) {
            System.out.println("isBasic: " + field.getName());
            System.out.println("getType: " + field.getType());
            System.out.println("getTypeName:" + t.getTypeName());
            System.out.println("t instanceof ParameterizedType:" + (t instanceof ParameterizedType ? true : false));
        }
        if (t instanceof ParameterizedType) {
            return false;
        }
        return isBasic(field.getType());
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

    private boolean isOptional(Field field) {
        return !Modifier.isFinal(field.getModifiers());
    }

    private String getPrimitiveType(Field field) {
        return getPrimitiveType(field.getType());
    }

    private String getPrimitiveType(Class clazz) {
        switch (clazz.getSimpleName().toLowerCase().replaceAll("(\\[|\\])*$", "")) {
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
//            case "object":
//                return "any";
            default:
                throw new RuntimeException("tip primitiv necunoscut:" + clazz.getSimpleName());
        }
    }

    private String convertAsObject(Field field, Consumer<Class<?>> newObjectReporter) {
        Type t = field.getGenericType();
        String name = field.getName();
        if (t instanceof ParameterizedType) {
            name = ((ParameterizedType) t).getRawType().getTypeName();
        } else {
            newObjectReporter.accept(field.getType());
        }
        return name + (isOptional(field) ? "?" : "") + ":" + field.getType().getSimpleName();
    }

    public static boolean shouldIncludeInterfaceOrClass(Class<?> c){
        return !c.getCanonicalName().startsWith("java.");
    }
}
