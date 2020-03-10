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
    implementation(fileTree(mapOf("dir" to "${project.rootDir}/ein/build/libs", "include" to listOf("ein-jvm.jar"))))

    implementation("org.springframework.boot:spring-boot-starter-quartz")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.security.oauth.boot:spring-security-oauth2-autoconfigure:2.2.1.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("com.squareup.okhttp3:okhttp:3.14.4")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.amazonaws:aws-java-sdk-core:1.11.703")
    implementation("com.amazonaws:aws-java-sdk-sqs:1.11.695")
    implementation("com.amazonaws:aws-java-sdk-s3:1.11.702")
    implementation("org.springframework.social:spring-social-facebook:2.0.3.RELEASE")
    implementation("com.github.spring-social:spring-social-google:1.1.3")
    implementation("redis.clients:jedis:3.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.4.4")
    implementation("org.apache.commons:commons-dbcp2:2.7.0")
    implementation("mysql:mysql-connector-java")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("javax.mail:mail:1.4.7")
    implementation("javax.mail:javax.mail-api:1.6.2")
    implementation("javax.activation:activation:1.1.1")

    implementation(kotlin("test-junit"))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
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
