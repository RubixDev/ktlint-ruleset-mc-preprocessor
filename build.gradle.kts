plugins {
    kotlin("jvm") version "1.9.22"
    `java-library`
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

repositories {
    mavenCentral()
}

group = "de.rubixdev.ktlint.mc.preprocessor"

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(tasks.classes)
    archiveClassifier = "sources"
    from(sourceSets.main.map { it.allSource })
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.javadoc)
    archiveClassifier = "javadoc"
    from(tasks.javadoc.map { it.destinationDir!! })
}

artifacts {
    archives(sourcesJar)
    archives(javadocJar)
}

dependencies {
    val ktlintCliVersion = "1.2.1"

    implementation("com.pinterest.ktlint:ktlint-cli-ruleset-core:$ktlintCliVersion")
    implementation("com.pinterest.ktlint:ktlint-rule-engine-core:$ktlintCliVersion")

    testImplementation("com.pinterest.ktlint:ktlint-test:$ktlintCliVersion")
    testImplementation("com.pinterest.ktlint:ktlint-ruleset-standard:$ktlintCliVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.12")

    ktlintRuleset(project)
}

ktlint {
    additionalEditorconfig =
        mapOf(
            "ktlint_standard_no-wildcard-imports" to "disabled",
            "ktlint_standard_chain-wrapping" to "disabled",
            "ktlint_standard_comment-spacing" to "disabled",
            "ktlint_standard_import-ordering" to "disabled",
        )
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
