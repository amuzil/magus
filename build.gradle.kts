/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
import cc.ekblad.toml.decode
import cc.ekblad.toml.model.TomlValue
import cc.ekblad.toml.tomlMapper
import com.fasterxml.jackson.databind.ObjectMapper
import io.gitlab.arturbosch.detekt.Detekt
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.kover.api.KoverPaths
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/* CONFIG */

data class ModsToml(
	val modLoader: String,
	val loaderVersion: VersionRange,
	val license: String = "All Rights Reserved",
	val issueTrackerURL: URL?,
	val mods: List<Mod>,
	val dependencies: Map<String, List<Dependency>>?
) {
	data class Mod(
		val modId: String,
		val version: String,
		val displayName: String,
		val updateJSONURL: URL?,
		val displayURL: URL?,
		val logoFile: String?,
		val credits: String?,
		val authors: String?,
		val displayTest: DisplayTest = DisplayTest.MATCH_VERSION,
		val description: String
	)

	data class Dependency(
		val modId: String,
		val mandatory: Boolean,
		val versionRange: VersionRange,
		val ordering: Ordering,
		val side: Side
	)

	enum class DisplayTest {
		MATCH_VERSION,
		IGNORE_SERVER_VERSION,
		IGNORE_ALL_VERSION,
		NONE
	}

	enum class Side {
		BOTH,
		CLIENT,
		SERVER
	}

	enum class Ordering {
		BEFORE,
		AFTER,
		NONE
	}

	data class VersionRange(
		val min: String,
		val max: String,
		val inclusiveMin: Boolean,
		val inclusiveMax: Boolean
	) {
		constructor(
			value: String
		) : this(
			value.substring(1, value.indexOf(',')),
			value.substring(value.indexOf(',') + 1, value.length - 1),
			value.startsWith("["),
			value.endsWith("]")
		)
	}

	companion object {
		fun mapper() = tomlMapper {
			decoder { it: TomlValue.String -> VersionRange(it.value) }
			decoder { it: TomlValue.String -> URL(it.value) }
			decoder { it: TomlValue.String ->
				// line ending backslash
				it.value.replace(Regex("\\\\(\\s*)"), "")
			}
		}
	}
}

/* SETTINGS */

// `gradle.properties`

// General information
val projectName: String by project
val modGroup: String by project
// POM information
val developerId: String by project
val developerName: String by project
val developerEmail: String by project
val scmConnection: String by project
val scmUrl: String by project
// Dependency versions
// languages
val javaVersion: String by project
val kotlinVersion: String by project
// build
// publish
// code quality
val prettierVersion: String by project
val prettierPluginTomlVersion: String by project
// documentation
// forge
val mixinProcessorVersion: String by project
// SonarQube settings
val sonarProjectKey: String by project
val sonarOrganization: String by project
val sonarHostUrl: String by project
// Librarian settings
val mappingsDate: String by project

// `mods.toml`

val config =
	ModsToml.mapper().decode<ModsToml>(file("src/main/resources/META-INF/mods.toml").toPath())
val modConfig = config.mods.first { it.modId == projectName }
val modDependencies = config.dependencies!![modConfig.modId]!!.associateBy { it.modId }

val forgeVersion = modDependencies["forge"]!!.versionRange.min
val minecraftVersion = modDependencies["minecraft"]!!.versionRange.min

/* PLUGINS */

plugins {
	// languages
	java
	kotlin("jvm")
	// build
	id("org.jetbrains.dokka")
	id("co.uzzu.dotenv.gradle")
	id("org.barfuin.gradle.taskinfo")
	// code quality
	pmd
	id("io.gitlab.arturbosch.detekt")
	id("org.jetbrains.kotlinx.kover")
	id("org.sonarqube")
	id("com.diffplug.spotless")
	// documentation
	// forge
	id("net.minecraftforge.gradle")
	id("org.parchmentmc.librarian.forgegradle")
	id("org.spongepowered.mixin")
	// publish
	`maven-publish`
	signing
}

/* GENERAL INFORMATION */

group = modGroup

description = modConfig.description

version = "${minecraftVersion}-${modConfig.version}"

/* DEPENDENCIES */

repositories {}

dependencies {
	jarJar("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", kotlinVersion)
	jarJar("org.jetbrains.kotlin", "kotlin-stdlib-jdk7", kotlinVersion)
	jarJar("org.jetbrains.kotlin", "kotlin-stdlib", kotlinVersion)
	jarJar("org.jetbrains.kotlin", "kotlin-stdlib-common", kotlinVersion)

	minecraft("net.minecraftforge", "forge", "${minecraftVersion}-${forgeVersion}")

	annotationProcessor(
		"org.spongepowered",
		"mixin",
		mixinProcessorVersion,
		classifier = "processor"
	)
}

/* LANGUAGES */

kotlin { jvmToolchain(javaVersion.toInt()) }

/* BUILD */

sourceSets { main { resources { srcDir("src/generated/resources") } } }

tasks.withType<KotlinCompile> { kotlinOptions { jvmTarget = javaVersion } }

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

// FIXME the ProcessResources task is incubating and thus deprecated
//  tasks.withType<ProcessResources> {
tasks.processResources {
	// Invalidate the task cache if the version changes
	inputs.property("version", version)

	filesMatching("META-INF/mods.toml") {
		filter {
			if (it.startsWith("version = ")) {
				"version = \"${project.version}\""
			} else {
				it
			}
		}
	}
}

tasks.withType<Jar> {
	outputs.upToDateWhen { false }

	archiveBaseName.set(modConfig.modId)

	duplicatesStrategy = DuplicatesStrategy.EXCLUDE

	manifest {
		attributes(
			"Specification-Title" to modConfig.displayName,
			"Specification-Vendor" to modConfig.authors,
			"Specification-Version" to project.version,
			"Implementation-Title" to modConfig.displayName,
			"Implementation-Vendor" to modConfig.authors,
			"Implementation-Version" to project.version,
			"Implementation-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
		)
	}
}

tasks.jar {
	archiveClassifier.set("thin")

	// Unnecessary, everything we want is in jarJar
	enabled = false
}

jarJar { enable() }

val reobfJarJar = reobf.create("jarJar")

tasks.jarJar {
	archiveClassifier.set("")

	finalizedBy(reobfJarJar)
}

afterEvaluate { tasks.assemble { setDependsOn(listOf(tasks.jarJar)) } }

/* CODE QUALITY */

detekt {
	parallel = true
	buildUponDefaultConfig = true
	config = files(".detekt.yaml")
}

tasks.withType<Detekt> {
	basePath = rootProject.projectDir.absolutePath

	reports { xml.required.set(true) }
}

sonarqube {
	properties {
		property("sonar.projectKey", sonarProjectKey)
		property("sonar.organization", sonarOrganization)
		property("sonar.host.url", sonarHostUrl)

		property("sonar.kotlin.file.suffixes", ".kt,.kts")

		// Analyse all files, including Kotlin (Gradle) scripts
		property("sonar.sources", ".")
		property("sonar.inclusions", "src/main/**/*, src/generated/**/*, *.kts")
		property("sonar.coverage.exclusions", "*.gradle.kts")

		// Other linters
		property(
			"sonar.java.pmd.reportPaths",
			listOf(tasks.pmdMain, tasks.pmdTest).joinToString(",") {
				it.get().reports.xml.outputLocation.get().asFile.path
			}
		)
		property(
			"sonar.kotlin.detekt.reportPaths",
			listOf(tasks.detekt).joinToString(",") { it.get().xmlReportFile.get().asFile.path }
		)
		property(
			"sonar.coverage.jacoco.xmlReportPaths",
			"${buildDir}/${KoverPaths.PROJECT_XML_REPORT_DEFAULT_PATH}"
		)
	}
}

tasks.sonar {
	dependsOn(tasks.pmdMain)
	dependsOn(tasks.pmdTest)
	dependsOn(tasks.detekt)
	dependsOn(tasks.koverXmlReport)
}

val lint by
	tasks.registering(Task::class) {
		group = "verification"
		description =
			"Runs all code quality checks. Requires the SONAR_TOKEN environment variable to be set."

		dependsOn(tasks.sonar)
	}

spotless {
	// Load from file and replace placeholders
	val licenseHeaderKt =
		file("license-header.kt")
			.readText()
			.replace("\$AUTHORS", modConfig.authors ?: throw Exception("Missing authors"))

	ratchetFrom("origin/main")

	kotlin {
		ktfmt().kotlinlangStyle()
		indentWithTabs(4)
		licenseHeader(licenseHeaderKt)
		toggleOffOn()
	}

	kotlinGradle {
		ktfmt().kotlinlangStyle()
		indentWithTabs(4)
		licenseHeader(licenseHeaderKt, "(pluginManagement |import )")
		toggleOffOn()
	}

	java {
		palantirJavaFormat()
		indentWithTabs(4)
		licenseHeader(licenseHeaderKt)
		toggleOffOn()
	}

	json {
		target("src/**/*.json", ".prettierrc")

		prettier(
				mapOf(
					"prettier" to prettierVersion,
				)
			)
			.apply { configFile(".prettierrc") }
	}

	format("toml") {
		target("**/*.toml")

		prettier(
				mapOf(
					"prettier" to prettierVersion,
					"prettier-plugin-toml" to prettierPluginTomlVersion
				)
			)
			.apply { configFile(".prettierrc") }
	}

	format("markdown") {
		target("**/*.md")

		prettier(
				mapOf(
					"prettier" to prettierVersion,
				)
			)
			.apply {
				configFile(".prettierrc")
				config(mapOf("tabWidth" to 2, "useTabs" to false))
			}
	}

	format("yaml") {
		target("**/*.yml", "**/*.yaml")

		prettier(
				mapOf(
					"prettier" to prettierVersion,
				)
			)
			.apply {
				configFile(".prettierrc")
				config(mapOf("tabWidth" to 2, "useTabs" to false))
			}
	}
}

val format by
	tasks.registering(Task::class) {
		group = "verification"
		description = "Runs the formatter on the project"

		dependsOn(tasks.spotlessApply)
	}

/* DOCUMENTATION */

val changelog by
	tasks.registering(Exec::class) {
		group = "changelog"
		description = "Generates a changelog for the current version. Requires PNPM"

		workingDir = project.rootDir

		ObjectMapper()
			.writeValue(
				file(".gitmoji-changelogrc"),
				mapOf(
					"project" to
						mapOf(
							"name" to modConfig.displayName,
							"description" to modConfig.description,
							"version" to project.version
						)
				)
			)

		val command =
			listOf(
				// spotless:off
                "pnpx", "gitmoji-changelog",
                "--format", "markdown",
                "--preset", "generic",
                "--output", "changelog.md",
                "--group-similar-commits", "true",
                "--author", "true"
                // spotless:on
			)

		with(OperatingSystem.current()) {
			when {
				isWindows -> commandLine(listOf("cmd", "/c") + command)
				isLinux -> commandLine(command)
				else -> throw IllegalStateException("Unsupported operating system: $this")
			}
		}

		finalizedBy("spotlessMarkdownApply")
	}

val dokkaJar by
	tasks.registering(Jar::class) {
		group = "documentation"
		description = "Generates the documentation as Dokka HTML"

		dependsOn(tasks.dokkaHtml)

		onlyIf { !tasks.dokkaHtml.get().state.upToDate }

		from(tasks.dokkaHtml.get().outputDirectory)

		archiveClassifier.set("dokka")
	}

val javadocJar by
	tasks.registering(Jar::class) {
		group = "documentation"
		description = "Generates the documentation as Javadoc HTML"

		dependsOn(tasks.dokkaJavadoc)

		onlyIf { !tasks.dokkaJavadoc.get().state.upToDate }

		from(tasks.dokkaJavadoc.get().outputDirectory)

		archiveClassifier.set("javadoc")
	}

val buildWithDocs by
	tasks.registering(Task::class) {
		group = "documentation"
		description = "Builds the project and generates the documentation"

		dependsOn(tasks.build)
		dependsOn(tasks.kotlinSourcesJar)
		dependsOn(dokkaJar)
		dependsOn(javadocJar)
	}

/* FORGE */

minecraft {
	mappings("parchment", "${mappingsDate}-${minecraftVersion}")

	accessTransformer("src/main/resources/META-INF/accesstransformer.cfg")

	runs {
		create("client") {
			workingDirectory("run/${name}")

			properties(
				mapOf(
					"forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP",
					"forge.logging.console.level" to "debug",
					"forge.enabledGameTestNamespaces" to modConfig.modId
				)
			)

			mods { create(modConfig.modId) { source(sourceSets.main.get()) } }
		}

		create("server") {
			workingDirectory("run/${name}")

			properties(
				mapOf(
					"forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP",
					"forge.logging.console.level" to "debug",
					"forge.enabledGameTestNamespaces" to modConfig.modId
				)
			)

			mods { create(modConfig.modId) { source(sourceSets.main.get()) } }
		}

		create("gameTestServer") {
			workingDirectory("run/${name}")

			properties(
				mapOf(
					"forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP",
					"forge.logging.console.level" to "debug",
					"forge.enabledGameTestNamespaces" to modConfig.modId
				)
			)

			mods { create(modConfig.modId) { source(sourceSets.main.get()) } }
		}

		create("data") {
			workingDirectory("run/${name}")

			properties(
				mapOf(
					"forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP",
					"forge.logging.console.level" to "debug",
					"forge.enabledGameTestNamespaces" to modConfig.modId
				)
			)

			args(
				// spotless:off
                "--all",
                "--mod", modConfig.modId,
                "--output", file("src/generated/resources"),
                "--existing", file("src/main/resources")
                // spotless:on
			)

			mods { create(modConfig.modId) { source(sourceSets.main.get()) } }
		}

		all {
			// Set the minecraft_classpath token to the paths of all jars in the library
			// configuration
			// This is added with the actual Minecraft classpath to get the real classpath
			// information later on
			lazyToken("minecraft_classpath") {
				configurations.jarJar
					.get()
					.copyRecursive()
					.resolve()
					.map { it.absolutePath }
					.filter { !(it.contains("org.jetbrains") && it.contains("annotations")) }
					.joinToString(File.pathSeparator)
			}
		}
	}
}

mixin {
	add(sourceSets.main.get(), "refmap.${modConfig.modId}.json")

	config("mixins.${modConfig.modId}.json")
}

/* PUBLISH */

tasks.publish {
	dependsOn(changelog)
	dependsOn(buildWithDocs)
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			from(components["java"])

			artifact(tasks.kotlinSourcesJar)
			artifact(dokkaJar)
			artifact(javadocJar)

			pom {
				name.set(modConfig.displayName)
				description.set(modConfig.description)
				url.set(modConfig.displayURL.toString())

				licenses {
					license {
						"(.*) <(.*)>".toRegex().find(config.license)!!.let {
							name.set(it.groupValues[1])
							url.set(it.groupValues[2])
						}
					}
				}

				developers {
					developer {
						id.set(developerId)
						name.set(developerName)
						email.set(developerEmail)
					}
				}

				scm {
					connection.set(scmConnection)
					developerConnection.set(scmConnection)
					url.set(scmUrl)
				}
			}
		}
	}

	repositories {
		if (env.GITHUB_ACTOR.isPresent && env.GITHUB_TOKEN.isPresent) {
			maven {
				name = "GitHubPackages"
				url = uri(scmUrl.replace("github.com", "maven.pkg.github.com"))
				credentials {
					username = env.GITHUB_ACTOR.value
					password = env.GITHUB_TOKEN.value
				}
			}
		} else if (System.getenv("GITHUB_ACTOR") != null && System.getenv("GITHUB_TOKEN") != null) {
			maven {
				name = "GitHubPackages"
				url = uri(scmUrl.replace("github.com", "maven.pkg.github.com"))
				credentials {
					username = System.getenv("GITHUB_ACTOR")
					password = System.getenv("GITHUB_TOKEN")
				}
			}
		}
	}
}

signing { sign(publishing.publications["maven"]) }
