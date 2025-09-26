plugins {
    `java-library`
    `maven-publish`
    id("org.openapi.generator") version "7.10.0"
}

group = "com.github.PAIR-Systems-Inc"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    // withJavadocJar() // Disabled due to invalid javadoc tags in generated code
}

repositories {
    mavenCentral()
}

dependencies {
    // OkHttp and Gson
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("io.gsonfire:gson-fire:1.9.0")
    
    // Jakarta WS-RS (for generated JSON class)
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    
    // misc
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
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
        // Fix default base path in ApiClient.java
        val apiClientFile = File("$buildDir/generated/src/main/java/ai/pairsys/jina4j/client/ApiClient.java")
        if (apiClientFile.exists()) {
            val content = apiClientFile.readText()
            val fixedContent = content
                .replace("private String basePath = \"http://localhost\";", "private String basePath = \"https://api.jina.ai\";")
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