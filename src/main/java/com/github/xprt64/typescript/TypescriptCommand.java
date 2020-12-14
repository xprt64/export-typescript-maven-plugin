package com.github.xprt64.typescript;

public class TypescriptCommand {
    public static String export(TypescriptInterface generatedInterface) {
        generatedInterface.addImport("dispatchCommand", "api_delegate");
        StringBuilder builder = new StringBuilder();

        String simpleName = generatedInterface.getSimpleName();
        String canonicalName = generatedInterface.getCanonicalName();

        builder.append(String.join("\n", generatedInterface.generateImports()));
        builder.append("\n");
        builder.append("\n");
        builder.append(generatedInterface.generateInterface());
        builder.append("\n");
        builder.append("\n" +
            "export async function dispatch" + simpleName + "(comanda: " + simpleName + ") {\n" +
            "    return await dispatch(comanda)\n" +
            "}\n" +
            "\n" +
            "export async function dispatch(comanda: " + simpleName + ") {\n" +
            "    return await dispatchCommand(\"" + canonicalName + "\", comanda)\n" +
            "}\n");


        return builder.toString();
    }
}
