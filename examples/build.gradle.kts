plugins {
    id("java")
    id("application")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// Disable caching for all Java compilation tasks
tasks.withType<JavaCompile> {
    outputs.cacheIf { false }
}

tasks.register<JavaExec>("runEmbeddingExample") {
    mainClass.set("ai.pairsys.jina4j.examples.EmbeddingExample")
    classpath = sourceSets["main"].runtimeClasspath
    description = "Run the embedding example"
}

tasks.register<JavaExec>("runRerankingExample") {
    mainClass.set("ai.pairsys.jina4j.examples.RerankingExample")
    classpath = sourceSets["main"].runtimeClasspath
    description = "Run the reranking example"
}

tasks.register<JavaExec>("runMultiVectorExample") {
    mainClass.set("ai.pairsys.jina4j.examples.MultiVectorExample")
    classpath = sourceSets["main"].runtimeClasspath
    description = "Run the multi-vector example"
}