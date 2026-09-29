// Módulo de dominio: Java puro, sin dependencias de Android.
// Contiene el modelo, los casos de uso, las reglas del NRUS y los puertos.
//
// OJO: este código corre en Android 8 (minSdk 26) y lint no lo revisa. Usa solo la biblioteca de Java 8
// más java.time: nada de Stream.toList(), List.of/copyOf, String.isBlank(), Optional.isEmpty() ni
// InputStream.readAllBytes() (existen en la JVM de las pruebas, pero no en los teléfonos antiguos).
plugins {
    `java-library`
    jacoco
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// Informe de cobertura en build/reports/jacoco/test/html/index.html.
tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

// RNF-18: cobertura de pruebas del paquete de reglas (motor NRUS y validadores) ≥ 80 %. Corre con `check`.
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf("pe.facturass20.dominio.reglas")
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
