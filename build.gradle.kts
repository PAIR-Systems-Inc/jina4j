plugins {
    `java-library`
    `maven-publish`
    id("org.openapi.generator") version "7.19.0"
}

group = "com.github.PAIR-Systems-Inc"
version = "v0.0.3"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    // withJavadocJar() // Disabled due to invalid javadoc tags in generated code
}

repositories {
    mavenCentral()
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    // OkHttp and Gson
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("io.gsonfire:gson-fire:1.9.0")
    
    // Jakarta WS-RS (for generated JSON class)
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:4.0.0")
    
    // misc
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.2")
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
}

tasks.test {
    useJUnitPlatform()
}

openApiGenerate {
    generatorName.set("java")
    library.set("okhttp-gson")
    inputSpec.set("$projectDir/openapi/jina-openapi.json")
    outputDir.set("$buildDir/generated")
    packageName.set("ai.pairsys.jina4j.client")
    apiPackage.set("ai.pairsys.jina4j.client.api")
    modelPackage.set("ai.pairsys.jina4j.client.model")
    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "useJakartaEe" to "true",
        "hideGenerationTimestamp" to "true",
        "generatePom" to "false",
        "openApiNullable" to "false",
        "legacyDiscriminatorBehavior" to "false",
        "disallowAdditionalPropertiesIfNotPresent" to "false"
    ))
}

sourceSets {
    main {
        java {
            srcDir("$buildDir/generated/src/main/java")
            exclude("**/auth/OAuth*")
        }
    }
}

tasks.register("fixGeneratedCode") {
    dependsOn("openApiGenerate")
    doLast {
        fun patchGeneratedFile(relativePath: String, patch: (String) -> String) {
            val file = File("$buildDir/generated/src/main/java/$relativePath")
            if (!file.exists()) return
            val original = file.readText()
            val updated = patch(original)
            if (updated != original) {
                file.writeText(updated)
            }
        }

        // Fix default base path in ApiClient.java
        val apiClientFile = File("$buildDir/generated/src/main/java/ai/pairsys/jina4j/client/ApiClient.java")
        if (apiClientFile.exists()) {
            val content = apiClientFile.readText()
            val fixedContent = content
                .replace("private String basePath = \"http://localhost\";", "private String basePath = \"https://api.jina.ai\";")
                .replace("protected String basePath = \"http://localhost\";", "protected String basePath = \"https://api.jina.ai\";")
                .replace("String basePath = \"http://localhost\";", "String basePath = \"https://api.jina.ai\";")
                .replace("protected Integer serverIndex = 0;", "protected Integer serverIndex = null;")
            apiClientFile.writeText(fixedContent)
        }
        
        // Fix Image.java
        val imageFile = File("$buildDir/generated/src/main/java/ai/pairsys/jina4j/client/model/Image.java")
        if (imageFile.exists()) {
            val content = imageFile.readText()
            val fixedContent = content
                .replace("URI.validateJsonElement(jsonElement);", "// URI validation not needed")
                .replace("File.validateJsonElement(jsonElement);", "// File validation not needed")
            imageFile.writeText(fixedContent)
        }
        
        // Fix all files that use java.io.File - replace with String
        val modelDir = File("$buildDir/generated/src/main/java/ai/pairsys/jina4j/client/model")
        modelDir.walk().filter { it.name.endsWith(".java") }.forEach { file ->
            val content = file.readText()
            if (content.contains("java.io.File") || content.contains(" File ")) {
                val fixedContent = content
                    .replace("import java.io.File;", "// import java.io.File; // Removed - using String instead")
                    .replace("File.class", "String.class")
                    .replace("TypeToken.get(File.class)", "TypeToken.get(String.class)")
                    .replace("final TypeAdapter<File>", "final TypeAdapter<String>")
                    .replace("(File)", "(String)")
                    .replace("instanceof File", "instanceof String")
                    .replace("public File get", "public String get")
                    .replace(": File", ": String")
                    .replace("<File>", "<String>")
                    .replace(" File ", " String ")
                    .replace("private File", "private String")
                    .replace("@Nullable File", "@Nullable String")
                file.writeText(fixedContent)
            }
        }
        
        // Fix Input.java serialization issue
        val inputFile = File("$buildDir/generated/src/main/java/ai/pairsys/jina4j/client/model/Input.java")
        if (inputFile.exists()) {
            val content = inputFile.readText()
            val fixedContent = content
                .replace(
                    "JsonPrimitive primitive = adapterListString.toJsonTree((List<String>)value.getActualInstance()).getAsJsonPrimitive();\n                        elementAdapter.write(out, primitive);",
                    "JsonElement element = adapterListString.toJsonTree((List<String>)value.getActualInstance());\n                        elementAdapter.write(out, element);"
                )
                .replace(
                    "JsonPrimitive primitive = adapterString.toJsonTree((String)value.getActualInstance()).getAsJsonPrimitive();\n                        elementAdapter.write(out, primitive);",
                    "JsonElement element = adapterString.toJsonTree((String)value.getActualInstance());\n                        elementAdapter.write(out, element);"
                )
            inputFile.writeText(fixedContent)
        }

        // Fix invalid Map anyOf/oneOf wrappers emitted by the Java generator for OpenAPI 3.1 map schemas.
        patchGeneratedFile("ai/pairsys/jina4j/client/model/Score.java") { content ->
            content
                .replace(
                    "final TypeAdapter<Map<String, BigDecimal>> adapterMap<String, BigDecimal> = gson.getDelegateAdapter(this, TypeToken.get(Map<String, BigDecimal>.class));",
                    "final Type typeInstanceMapStringBigDecimal = new TypeToken<Map<String, BigDecimal>>(){}.getType();\n            final TypeAdapter<Map<String, BigDecimal>> adapterMapStringBigDecimal = (TypeAdapter<Map<String, BigDecimal>>) gson.getDelegateAdapter(this, TypeToken.get(typeInstanceMapStringBigDecimal));"
                )
                .replace("instanceof Map<String, BigDecimal>", "instanceof Map<?, ?>")
                .replace(
                    "Map<String, BigDecimal>.validateJsonElement(jsonElement);",
                    "if (!jsonElement.isJsonObject()) { throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected json element to be an object in the JSON string but got `%s`\", jsonElement.toString())); }"
                )
        }

        patchGeneratedFile("ai/pairsys/jina4j/client/model/Prediction.java") { content ->
            content
                .replace(
                    "final TypeAdapter<Map<String, String>> adapterMap<String, String> = gson.getDelegateAdapter(this, TypeToken.get(Map<String, String>.class));",
                    "final Type typeInstanceMapStringString = new TypeToken<Map<String, String>>(){}.getType();\n            final TypeAdapter<Map<String, String>> adapterMapStringString = (TypeAdapter<Map<String, String>>) gson.getDelegateAdapter(this, TypeToken.get(typeInstanceMapStringString));"
                )
                .replace(
                    "JsonPrimitive primitive = adapterMapStringString.toJsonTree((Map<String, String>)value.getActualInstance()).getAsJsonPrimitive();\n                        elementAdapter.write(out, primitive);",
                    "JsonElement element = adapterMapStringString.toJsonTree((Map<String, String>)value.getActualInstance());\n                        elementAdapter.write(out, element);"
                )
                .replace("instanceof Map<String, String>", "instanceof Map<?, ?>")
                .replace(
                    "if (!jsonElement.getAsJsonPrimitive().isNumber()) {\n                            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected json element to be of type Number in the JSON string but got `%s`\", jsonElement.toString()));\n                        }",
                    "if (!jsonElement.isJsonObject()) {\n                            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected json element to be an object in the JSON string but got `%s`\", jsonElement.toString()));\n                        }"
                )
        }

        patchGeneratedFile("ai/pairsys/jina4j/client/model/Labels.java") { content ->
            content
                .replace(
                    "final TypeAdapter<Map<String, List<String>>> adapterMap<String, List<String>> = gson.getDelegateAdapter(this, TypeToken.get(Map<String, List<String>>.class));",
                    "final Type typeInstanceMapStringListString = new TypeToken<Map<String, List<String>>>(){}.getType();\n            final TypeAdapter<Map<String, List<String>>> adapterMapStringListString = (TypeAdapter<Map<String, List<String>>>) gson.getDelegateAdapter(this, TypeToken.get(typeInstanceMapStringListString));"
                )
                .replace(
                    "JsonPrimitive primitive = adapterListString.toJsonTree((List<String>)value.getActualInstance()).getAsJsonPrimitive();\n                        elementAdapter.write(out, primitive);",
                    "JsonElement element = adapterListString.toJsonTree((List<String>)value.getActualInstance());\n                        elementAdapter.write(out, element);"
                )
                .replace("instanceof Map<String, List<String>>", "instanceof Map<?, ?>")
                .replace(
                    "Map<String, List<String>>.validateJsonElement(jsonElement);",
                    "if (!jsonElement.isJsonObject()) { throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected json element to be an object in the JSON string but got `%s`\", jsonElement.toString())); }"
                )
        }

        patchGeneratedFile("ai/pairsys/jina4j/client/model/Predictions.java") { content ->
            content
                .replace(
                    "final TypeAdapter<Map<String, List<ClassificationPredictionLabel>>> adapterMap<String, List<ClassificationPredictionLabel>> = gson.getDelegateAdapter(this, TypeToken.get(Map<String, List<ClassificationPredictionLabel>>.class));",
                    "final Type typeInstanceMapStringListClassificationPredictionLabel = new TypeToken<Map<String, List<ClassificationPredictionLabel>>>(){}.getType();\n            final TypeAdapter<Map<String, List<ClassificationPredictionLabel>>> adapterMapStringListClassificationPredictionLabel = (TypeAdapter<Map<String, List<ClassificationPredictionLabel>>>) gson.getDelegateAdapter(this, TypeToken.get(typeInstanceMapStringListClassificationPredictionLabel));"
                )
                .replace("instanceof Map<String, List<ClassificationPredictionLabel>>", "instanceof Map<?, ?>")
                .replace(
                    "Map<String, List<ClassificationPredictionLabel>>.validateJsonElement(jsonElement);",
                    "if (!jsonElement.isJsonObject()) { throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected json element to be an object in the JSON string but got `%s`\", jsonElement.toString())); }"
                )
        }

        // Fix anyOf wrapper handling for embedding response payloads.
        patchGeneratedFile("ai/pairsys/jina4j/client/model/Embedding.java") { content ->
            content
                .replace("if (!list.isEmpty() && list.get(0) instanceof BigDecimal) {", "if (list.isEmpty() || list.get(0) instanceof BigDecimal) {")
                .replace("if (!jsonElement.getAsJsonPrimitive().isNumber()) {", "if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {")
        }

        patchGeneratedFile("ai/pairsys/jina4j/client/model/Data.java") { content ->
            content
                .replace(
                    "                    // check if the actual instance is of the type `List<SingleEmbeddingData>`\n" +
                        "                    if (value.getActualInstance() instanceof List<?>) {\n" +
                        "                        List<?> list = (List<?>) value.getActualInstance();\n" +
                        "                        if (!list.isEmpty() && list.get(0) instanceof SingleEmbeddingData) {\n" +
                        "                            JsonArray array = adapterListSingleEmbeddingData.toJsonTree((List<SingleEmbeddingData>)value.getActualInstance()).getAsJsonArray();\n" +
                        "                            elementAdapter.write(out, array);\n" +
                        "                            return;\n" +
                        "                        }\n" +
                        "                    }\n",
                    "                    if (value.getActualInstance() instanceof List<?>) {\n" +
                        "                        List<?> list = (List<?>) value.getActualInstance();\n" +
                        "                        if (list.isEmpty() || list.get(0) instanceof SingleEmbeddingData) {\n" +
                        "                            JsonArray array = adapterListSingleEmbeddingData.toJsonTree((List<SingleEmbeddingData>)value.getActualInstance()).getAsJsonArray();\n" +
                        "                            elementAdapter.write(out, array);\n" +
                        "                            return;\n" +
                        "                        }\n" +
                        "                        if (list.get(0) instanceof MultiEmbeddingData) {\n" +
                        "                            JsonArray array = adapterListMultiEmbeddingData.toJsonTree((List<MultiEmbeddingData>)value.getActualInstance()).getAsJsonArray();\n" +
                        "                            elementAdapter.write(out, array);\n" +
                        "                            return;\n" +
                        "                        }\n" +
                        "                    }\n"
                )
                .replace(
                    "                    // deserialize List<SingleEmbeddingData>\n",
                    "                    // deserialize List<MultiEmbeddingData>\n" +
                        "                    try {\n" +
                        "                        // validate the JSON object to see if any exception is thrown\n" +
                        "                        if (!jsonElement.isJsonArray()) {\n" +
                        "                            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected json element to be a array type in the JSON string but got `%s`\", jsonElement.toString()));\n" +
                        "                        }\n" +
                        "\n" +
                        "                        JsonArray array = jsonElement.getAsJsonArray();\n" +
                        "                        // validate array items\n" +
                        "                        for(JsonElement element : array) {\n" +
                        "                            MultiEmbeddingData.validateJsonElement(element);\n" +
                        "                        }\n" +
                        "                        actualAdapter = adapterListMultiEmbeddingData;\n" +
                        "                        Data ret = new Data();\n" +
                        "                        ret.setActualInstance(actualAdapter.fromJsonTree(jsonElement));\n" +
                        "                        return ret;\n" +
                        "                    } catch (Exception e) {\n" +
                        "                        // deserialization failed, continue\n" +
                        "                        errorMessages.add(String.format(java.util.Locale.ROOT, \"Deserialization for List<MultiEmbeddingData> failed with `%s`.\", e.getMessage()));\n" +
                        "                        log.log(Level.FINER, \"Input data does not match schema 'List<MultiEmbeddingData>'\", e);\n" +
                        "                    }\n" +
                        "\n" +
                        "                    // deserialize List<SingleEmbeddingData>\n"
                )
                .replace(
                    "        schemas.put(\"List<SingleEmbeddingData>\", List.class);",
                    "        schemas.put(\"List<SingleEmbeddingData>\", List.class);\n" +
                        "        schemas.put(\"List<MultiEmbeddingData>\", List.class);"
                )
                .replace(
                    "        if (instance instanceof List<?>) {\n" +
                        "            List<?> list = (List<?>) instance;\n" +
                        "            if (!list.isEmpty() && list.get(0) instanceof SingleEmbeddingData) {\n" +
                        "                super.setActualInstance(instance);\n" +
                        "                return;\n" +
                        "            }\n" +
                        "        }\n",
                    "        if (instance instanceof List<?>) {\n" +
                        "            List<?> list = (List<?>) instance;\n" +
                        "            if (list.isEmpty() || list.get(0) instanceof SingleEmbeddingData || list.get(0) instanceof MultiEmbeddingData) {\n" +
                        "                super.setActualInstance(instance);\n" +
                        "                return;\n" +
                        "            }\n" +
                        "        }\n"
                )
                .replace(
                    "        // validate the json string with List<SingleEmbeddingData>\n",
                    "        // validate the json string with List<MultiEmbeddingData>\n" +
                        "        try {\n" +
                        "            if (!jsonElement.isJsonArray()) {\n" +
                        "                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected json element to be a array type in the JSON string but got `%s`\", jsonElement.toString()));\n" +
                        "            }\n" +
                        "            JsonArray array = jsonElement.getAsJsonArray();\n" +
                        "            // validate array items\n" +
                        "            for(JsonElement element : array) {\n" +
                        "                MultiEmbeddingData.validateJsonElement(element);\n" +
                        "            }\n" +
                        "            return;\n" +
                        "        } catch (Exception e) {\n" +
                        "            errorMessages.add(String.format(java.util.Locale.ROOT, \"Deserialization for List<MultiEmbeddingData> failed with `%s`.\", e.getMessage()));\n" +
                        "            // continue to the next one\n" +
                        "        }\n" +
                        "        // validate the json string with List<SingleEmbeddingData>\n"
                )
        }

        patchGeneratedFile("ai/pairsys/jina4j/client/model/Embeddings.java") { content ->
            content
                .replace(
                    "                    // check if the actual instance is of the type `List<String>`\n" +
                        "                    if (value.getActualInstance() instanceof List<?>) {\n" +
                        "                        JsonPrimitive primitive = adapterListString.toJsonTree((List<String>)value.getActualInstance()).getAsJsonPrimitive();\n" +
                        "                        elementAdapter.write(out, primitive);\n" +
                        "                        return;\n" +
                        "                    }\n",
                    "                    if (value.getActualInstance() instanceof List<?>) {\n" +
                        "                        List<?> list = (List<?>) value.getActualInstance();\n" +
                        "                        if (list.isEmpty() || list.get(0) instanceof String) {\n" +
                        "                            JsonArray array = adapterListString.toJsonTree((List<String>)value.getActualInstance()).getAsJsonArray();\n" +
                        "                            elementAdapter.write(out, array);\n" +
                        "                            return;\n" +
                        "                        }\n" +
                        "                        if (list.get(0) instanceof List<?>) {\n" +
                        "                            JsonArray array = adapterListListBigDecimal.toJsonTree((List<List<BigDecimal>>)value.getActualInstance()).getAsJsonArray();\n" +
                        "                            elementAdapter.write(out, array);\n" +
                        "                            return;\n" +
                        "                        }\n" +
                        "                    }\n"
                )
                .replace(
                    "                    // deserialize List<String>\n",
                    "                    // deserialize List<List<BigDecimal>>\n" +
                        "                    try {\n" +
                        "                        // validate the JSON object to see if any exception is thrown\n" +
                        "                        if (!jsonElement.isJsonArray()) {\n" +
                        "                            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected json element to be a array type in the JSON string but got `%s`\", jsonElement.toString()));\n" +
                        "                        }\n" +
                        "\n" +
                        "                        JsonArray array = jsonElement.getAsJsonArray();\n" +
                        "                        // validate array items\n" +
                        "                        for(JsonElement element : array) {\n" +
                        "                            if (!element.isJsonArray()) {\n" +
                        "                                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected array items to be arrays in the JSON string but got `%s`\", jsonElement.toString()));\n" +
                        "                            }\n" +
                        "                            for (JsonElement innerElement : element.getAsJsonArray()) {\n" +
                        "                                if (!innerElement.isJsonPrimitive() || !innerElement.getAsJsonPrimitive().isNumber()) {\n" +
                        "                                    throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected nested array items to be of type Number in the JSON string but got `%s`\", jsonElement.toString()));\n" +
                        "                                }\n" +
                        "                            }\n" +
                        "                        }\n" +
                        "                        actualAdapter = adapterListListBigDecimal;\n" +
                        "                        Embeddings ret = new Embeddings();\n" +
                        "                        ret.setActualInstance(actualAdapter.fromJsonTree(jsonElement));\n" +
                        "                        return ret;\n" +
                        "                    } catch (Exception e) {\n" +
                        "                        // deserialization failed, continue\n" +
                        "                        errorMessages.add(String.format(java.util.Locale.ROOT, \"Deserialization for List<List<BigDecimal>> failed with `%s`.\", e.getMessage()));\n" +
                        "                        log.log(Level.FINER, \"Input data does not match schema 'List<List<BigDecimal>>'\", e);\n" +
                        "                    }\n" +
                        "\n" +
                        "                    // deserialize List<String>\n"
                )
                .replace(
                    "if (!element.getAsJsonPrimitive().isString()) {",
                    "if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {"
                )
                .replace(
                    "        schemas.put(\"List<String>\", List.class);",
                    "        schemas.put(\"List<String>\", List.class);\n" +
                        "        schemas.put(\"List<List<BigDecimal>>\", List.class);"
                )
                .replace(
                    "        if (instance instanceof List<?>) {\n" +
                        "            List<?> list = (List<?>) instance;\n" +
                        "            if (!list.isEmpty() && list.get(0) instanceof String) {\n" +
                        "                super.setActualInstance(instance);\n" +
                        "                return;\n" +
                        "            }\n" +
                        "        }\n",
                    "        if (instance instanceof List<?>) {\n" +
                        "            List<?> list = (List<?>) instance;\n" +
                        "            if (list.isEmpty() || list.get(0) instanceof String || list.get(0) instanceof List<?>) {\n" +
                        "                super.setActualInstance(instance);\n" +
                        "                return;\n" +
                        "            }\n" +
                        "        }\n"
                )
                .replace(
                    "        // validate the json string with List<String>\n",
                    "        // validate the json string with List<List<BigDecimal>>\n" +
                        "        try {\n" +
                        "            if (!jsonElement.isJsonArray()) {\n" +
                        "                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected json element to be a array type in the JSON string but got `%s`\", jsonElement.toString()));\n" +
                        "            }\n" +
                        "            JsonArray array = jsonElement.getAsJsonArray();\n" +
                        "            // validate array items\n" +
                        "            for(JsonElement element : array) {\n" +
                        "                if (!element.isJsonArray()) {\n" +
                        "                    throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected array items to be arrays in the JSON string but got `%s`\", jsonElement.toString()));\n" +
                        "                }\n" +
                        "                for (JsonElement innerElement : element.getAsJsonArray()) {\n" +
                        "                    if (!innerElement.isJsonPrimitive() || !innerElement.getAsJsonPrimitive().isNumber()) {\n" +
                        "                        throw new IllegalArgumentException(String.format(java.util.Locale.ROOT, \"Expected nested array items to be of type Number in the JSON string but got `%s`\", jsonElement.toString()));\n" +
                        "                    }\n" +
                        "                }\n" +
                        "            }\n" +
                        "            return;\n" +
                        "        } catch (Exception e) {\n" +
                        "            errorMessages.add(String.format(java.util.Locale.ROOT, \"Deserialization for List<List<BigDecimal>> failed with `%s`.\", e.getMessage()));\n" +
                        "            // continue to the next one\n" +
                        "        }\n" +
                        "        // validate the json string with List<String>\n"
                )
        }
    }
}

tasks.compileJava {
    dependsOn("fixGeneratedCode")
    outputs.cacheIf { false }  // Disable caching for generated code
}

// Disable caching for all Java compilation tasks
tasks.withType<JavaCompile> {
    outputs.cacheIf { false }
}

tasks.named<Jar>("sourcesJar") {
    dependsOn("fixGeneratedCode")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/PAIR-Systems-Inc/jina4j")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("Jina4J")
                description.set("Java client library for Jina AI's Embedding and Reranking API")
                url.set("https://github.com/PAIR-Systems-Inc/jina4j")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("pair-systems")
                        name.set("PAIR Systems Inc")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/PAIR-Systems-Inc/jina4j.git")
                    developerConnection.set("scm:git:ssh://github.com/PAIR-Systems-Inc/jina4j.git")
                    url.set("https://github.com/PAIR-Systems-Inc/jina4j")
                }
            }
        }
    }
}
