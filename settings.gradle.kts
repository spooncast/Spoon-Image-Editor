import java.io.FileInputStream
import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val secretsPropertiesFile = rootDir.resolve("secrets.properties")
val secretProp = Properties()
if (secretsPropertiesFile.exists()) {
    FileInputStream(secretsPropertiesFile).use { secretProp.load(it) }
} else {
    secretProp.setProperty("JFROG_USERNAME", System.getenv("JFROG_USERNAME") ?: "")
    secretProp.setProperty("JFROG_PASSWORD", System.getenv("JFROG_PASSWORD") ?: "")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            credentials {
                username = secretProp.getProperty("JFROG_USERNAME")
                password = secretProp.getProperty("JFROG_PASSWORD")
            }
            url = uri("https://spoonradio.jfrog.io/artifactory/spoonandroid-designsystem")
        }
    }
}

rootProject.name = "Spoon-Image-Editor"
include(":crop", ":sample")
