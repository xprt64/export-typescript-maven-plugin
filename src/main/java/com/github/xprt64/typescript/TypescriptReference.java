package com.github.xprt64.typescript;

public class TypescriptReference {
    public static void export(TypescriptInterface generatedInterface, String apiDir) {
        String code = generateCode(generatedInterface);
        FileWriter.writeToFile(generatedInterface, code, apiDir, ".ts");
    }

    public static String generateCode(TypescriptInterface generatedInterface) {
        StringBuilder builder = new StringBuilder();

        builder.append(String.join("\n", generatedInterface.generateImports()));
        builder.append("\n");
        builder.append("\n");
        builder.append(generatedInterface.generateInterface());


        return builder.toString();
    }
}
