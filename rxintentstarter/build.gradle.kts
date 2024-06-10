plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
}

val versionStr = "1.0.0"

fun String.runCommand(workingDir: File = file("./")): String {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    return proc.inputStream.bufferedReader().readText().trim()
}

fun getCurrentGitBranch(): String {
    var gitBranch = "Unknown branch"
    try {
        val workingDir = project.projectDir
        val result = "git rev-parse --abbrev-ref HEAD".runCommand(workingDir)
        gitBranch = result.trim()
    } catch (e: Exception) {
    }
    return gitBranch
}

fun getUserName() = System.getProperty("user.name")

val group = "com.algorigo.rx"
val archivesBaseName = "rxintentstarter"

val versionName = if (getCurrentGitBranch().contains("main")) {
    versionStr
} else {
    "$versionStr-${getUserName()}-SNAPSHOT"
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = group
            artifactId = archivesBaseName
            version = versionName
            pom {
                name.set("RxIntentStarter")
                description.set("A library for using startActivity and startActivityForResult based on RxJava")
                url.set("https://github.com/Algorigo/RxIntentStarter")
                artifact("$buildDir/outputs/aar/${project.name}-release.aar")

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("rouddy")
                        name.set("Rouddy")
                        email.set("rouddy@naver.com")
                    }
                }

                scm {
                    connection.set("scm:https://github.com/Algorigo/RxIntentStarter.git")
                    developerConnection.set("scm:git:https://github.com/Algorigo/RxIntentStarter.git")
                    url.set("https://github.com/Algorigo/RxIntentStarter")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri(if (versionName.endsWith("SNAPSHOT")) {
                findProperty("NEXUS_SNAPSHOT_REPOSITORY_URL") as String
            } else {
                findProperty("NEXUS_REPOSITORY_URL") as String
            })
            credentials {
                username = findProperty("nexusUsername") as String
                password = findProperty("nexusPassword") as String
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

android {
    namespace = "com.algorigo.rxintentstarter"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //ReactiveX
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    //RxKotlin
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
}