package com.github.xprt64.typescript;

public class TypescriptQuestion {
    public static String export(TypescriptInterface generatedInterface) {
        generatedInterface.addImport("answerQuestion", "api_delegate");
        StringBuilder builder = new StringBuilder();

        String simpleName = generatedInterface.getSimpleName();
        String canonicalName = generatedInterface.getCanonicalName();

        builder.append(String.join("\n", generatedInterface.generateImports()));
        builder.append("\n");
        builder.append("\n");
        builder.append(generatedInterface.generateInterface());
        builder.append("\n");
        builder.append("\n" +
            "export async function question" + simpleName + "(question: " + simpleName + "): " + simpleName + " {\n" +
            "    return await question(question)\n" +
            "}\n" +
            "\n" +
            "export async function question(question: " + simpleName + "): " + simpleName + " {\n" +
            "    return await answerQuestion(\"" + canonicalName + "\", question)\n" +
            "}\n");


        return builder.toString();
    }
}
