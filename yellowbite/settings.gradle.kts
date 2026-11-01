pluginManagement {
    repositories { pluginManagement {
        repositories {
            // 添加阿里云镜像
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                // 同样添加阿里云镜像
                maven { url = uri("https://maven.aliyun.com/repository/public") }
                maven { url = uri("https://maven.aliyun.com/repository/google") }

                google()
                mavenCentral()
            }
        }
        google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { pluginManagement {
        repositories {
            // 添加阿里云镜像
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                // 同样添加阿里云镜像
                maven { url = uri("https://maven.aliyun.com/repository/public") }
                maven { url = uri("https://maven.aliyun.com/repository/google") }

                google()
                mavenCentral()
            }
        }
        google(); mavenCentral() }
}
rootProject.name = "YellowBite"
include(":app")
