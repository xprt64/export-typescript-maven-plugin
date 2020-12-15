package com.github.xprt64.typescript;

public class TypescriptQuestion {
    public static void export(TypescriptInterface generatedInterface, FileExporter fileExporter) {
        String code = generateCode(generatedInterface);
        fileExporter.writeFile(generatedInterface.getRelativeDirPath() + "/" + generatedInterface.getFileName() + ".ts", code);
    }

    public static String generateCode(TypescriptInterface generatedInterface) {
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
            "export async function askQuestion" + simpleName + "(question: " + simpleName + "): Promise<" + simpleName + "> {\n" +
            "    return await askQuestion(question)\n" +
            "}\n" +
            "\n" +
            "export async function askQuestion(question: " + simpleName + "): Promise<" + simpleName + "> {\n" +
            "    return await answerQuestion(\"" + canonicalName + "\", question)\n" +
            "}\n");


        return builder.toString();
    }
}
