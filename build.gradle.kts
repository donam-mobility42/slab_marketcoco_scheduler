import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.2.4.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    war
    kotlin("plugin.spring") version "1.3.61"
    kotlin("jvm")
}

springBoot{
    mainClassName = "com.slab.marketcoco.MarketcocoSchedulerApp"
}

group = "com.slab.marketcoco"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}
dependencies {
    api(project(":ein"))
    //api(project(":marketcoco"))

    implementation(fileTree(mapOf("dir" to "${project.rootDir}/ein/build/libs", "include" to listOf("ein-jvm.jar"))))
    //implementation(fileTree(mapOf("dir" to "${project.rootDir}/marketcoco/build/libs", "include" to listOf("marketcoco-0.0.1.jar"))))
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    //implementation(kotlin("test-junit"))
    //testImplementation("org.springframework.boot:spring-boot-starter-test") {
    //    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    //}
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

//Build된 War를 ROOT.war로 만든다.
//이 war는 Git으로 배포하고 실서버에서 git pull해서 사용하게 된다.
//clean bootWar copyWar --stacktrace
task("copyWar"){
    doLast{
        val warName = "${project.name}-${project.version}.war"
        val f = "$buildDir/libs/"
        val t = "$buildDir/../../../slab_war/marketcoco_scheduler/"
        copy{
            from("$f$warName")
            into(t)
            rename(warName, "ROOT.war")
        }
    }
}
