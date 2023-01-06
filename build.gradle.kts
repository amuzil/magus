/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
import cc.ekblad.toml.decode
import cc.ekblad.toml.model.TomlValue
import cc.ekblad.toml.tomlMapper
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
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

val config =
	ModsToml.mapper().decode<ModsToml>(file("src/main/resources/META-INF/mods.toml").toPath())

// Settings from `mods.toml`
val modConfig = config.mods[0]
val modDependencies = config.dependencies!![modConfig.modId]!!.associateBy { it.modId }

val forgeVersion = modDependencies["forge"]!!.versionRange.min
val minecraftVersion = modDependencies["minecraft"]!!.versionRange.min

// Settings from `settings.gradle.kts`
val modGroup: String by project
val developerId: String by project
val developerName: String by project
val developerEmail: String by project
val scmConnection: String by project
val scmUrl: String by project

val javaVersion: String by project
val kotlinVersion: String by project
val mixinProcessorVersion: String by project
val mappingsDate: String by project
val prettierVersion: String by project
val prettierPluginTomlVersion: String by project

/* PLUGINS */

plugins {
	java
	`maven-publish`
	signing
	id("org.jetbrains.kotlin.jvm")
	id("com.diffplug.spotless")
	id("org.jetbrains.dokka")
	id("org.barfuin.gradle.taskinfo")
	id("co.uzzu.dotenv.gradle")
	id("net.minecraftforge.gradle")
	id("org.parchmentmc.librarian.forgegradle")
	id("org.spongepowered.mixin")
}

/* PROJECT INFORMATION */

group = modGroup

description = modConfig.description

version = "${minecraftVersion}-${modConfig.version}"

/* DEPENDENCIES */

repositories {}

dependencies {
	minecraft("net.minecraftforge", "forge", "${minecraftVersion}-${forgeVersion}")

	jarJar("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", kotlinVersion)
	jarJar("org.jetbrains.kotlin", "kotlin-stdlib-jdk7", kotlinVersion)
	jarJar("org.jetbrains.kotlin", "kotlin-stdlib", kotlinVersion)
	jarJar("org.jetbrains.kotlin", "kotlin-stdlib-common", kotlinVersion)

	annotationProcessor(
		"org.spongepowered",
		"mixin",
		mixinProcessorVersion,
		classifier = "processor"
	)
}

/* COMPILATION */

kotlin { jvmToolchain(javaVersion.toInt()) }

tasks.withType<KotlinCompile> { kotlinOptions { jvmTarget = javaVersion } }

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

/* PLUGIN CONFIGURATION */

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
}

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

sourceSets { main { resources { srcDir("src/generated/resources") } } }

/* TASKS */

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

tasks.jar { finalizedBy("reobfJar") }

jarJar { enable() }

tasks.jarJar { archiveClassifier.set("") }

reobf.create("jarJar")

tasks.build { dependsOn(tasks.jarJar) }

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

val format =
	tasks.create("format") {
		group = "verification"
		description = "Runs the formatter on the project"

		dependsOn(tasks.spotlessApply)
	}

val dokkaJar by
	tasks.registering(Jar::class) {
		group = "documentation"
		description = "Generates the documentation as Dokka HTML"

		dependsOn(tasks.dokkaHtml)
		from(tasks.dokkaHtml.get().outputDirectory)

		archiveClassifier.set("dokka")
	}

val javadocJar by
	tasks.registering(Jar::class) {
		group = "documentation"
		description = "Generates the documentation as Javadoc HTML"

		dependsOn(tasks.dokkaJavadoc)
		from(tasks.dokkaJavadoc.get().outputDirectory)

		archiveClassifier.set("javadoc")
	}

tasks.build {
	dependsOn(tasks.kotlinSourcesJar)
	dependsOn(dokkaJar)
	dependsOn(javadocJar)
}

/* PUBLISHING */

tasks.publish { dependsOn(changelog) }

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
		}
	}
}

signing { sign(publishing.publications["maven"]) }
