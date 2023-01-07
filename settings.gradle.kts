/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
import java.net.URL

pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven("https://maven.minecraftforge.net")
		maven("https://maven.parchmentmc.org")
	}

	// languages
	val kotlinVersion: String by settings
	// build
	val dokkaVersion: String by settings
	val dotenvVersion: String by settings
	val taskinfoVersion: String by settings
	// code quality
	val detektVersion: String by settings
	val koverVersion: String by settings
	val sonarqubeVersion: String by settings
	val spotlessVersion: String by settings
	// documentation
	// forge
	val forgeGradleVersion: String by settings
	val librarianVersion: String by settings
	// publish

	plugins {
		// languages
		kotlin("jvm") version kotlinVersion
		// build
		id("org.jetbrains.dokka") version dokkaVersion
		id("co.uzzu.dotenv.gradle") version dotenvVersion
		id("org.barfuin.gradle.taskinfo") version taskinfoVersion
		// publish
		// code quality
		id("io.gitlab.arturbosch.detekt") version detektVersion
		id("org.jetbrains.kotlinx.kover") version koverVersion
		id("org.sonarqube") version sonarqubeVersion
		id("com.diffplug.spotless") version spotlessVersion
		// documentation
		// forge
		id("net.minecraftforge.gradle") version forgeGradleVersion
		id("org.parchmentmc.librarian.forgegradle") version librarianVersion
	}
}

buildscript {
	repositories {
		maven("https://repo.spongepowered.org/repository/maven-public/")
		maven("https://jitpack.io")
	}

	// languages
	// build
	val komaVersion: String by settings
	// code quality
	// documentation
	// forge
	val mixinGradleVersion: String by settings
	// publish

	dependencies {
		classpath("org.spongepowered", "mixingradle", mixinGradleVersion)
		classpath("cc.ekblad", "4koma", komaVersion)
	}
}

val projectName: String by settings

rootProject.name = projectName

plugins { id("org.danilopianini.gradle-pre-commit-git-hooks") version "[1.1.1,2)" }

gitHooks {
	preCommit {
		from {
			"""
			./gradlew check
			"""
				.trimIndent()
		}
	}

	commitMsg {
		from {
			URL(
					"https://gist.githubusercontent.com/mahtaran/b202b92a26fdd52c78197e7373cb3a91/raw/amuzil-commit-msg-git-hook.sh"
				)
				.readText()
		}
	}

	createHooks(true)
}
