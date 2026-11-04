package com.modestoneto.secretaria;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.modestoneto.secretaria", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule dominioSemFrameworks = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.hibernate..",
                    "tools.jackson..", "com.fasterxml.jackson..", "io.swagger..");

    @ArchTest
    static final ArchRule aplicacaoSemSpringDataNemJpa = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.data..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule nucleoNaoDependeDeAdaptadores = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");
}
