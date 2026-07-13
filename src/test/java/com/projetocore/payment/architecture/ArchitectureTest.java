package com.projetocore.payment.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Aplicação automática do Princípio I (Clean Architecture) da constituição.
 */
class ArchitectureTest {

    private static com.tngtech.archunit.core.domain.JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.projetocore.payment");
    }

    @Test
    void domainNaoDependeDeApplicationNemInfrastructure() {
        ArchRule regra = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..");
        regra.check(classes);
    }

    @Test
    void domainNaoDependeDeSpringNemJpaNemHttp() {
        ArchRule regra = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "org.springframework.web.reactive..");
        regra.check(classes);
    }

    @Test
    void applicationNaoDependeDeInfrastructure() {
        ArchRule regra = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
        regra.check(classes);
    }

    @Test
    void applicationNaoDependeDeSpring() {
        ArchRule regra = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..");
        regra.check(classes);
    }

    @Test
    void controllersFicamSomenteEmInfrastructureAdapterHttp() {
        ArchRule regra = classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().resideInAPackage("..infrastructure.adapter.http..");
        regra.check(classes);
    }

    @Test
    void portasFicamSomenteEmDomainPort() {
        ArchRule regra = classes()
                .that().haveSimpleNameEndingWith("Port")
                .should().resideInAPackage("..domain.port..");
        regra.check(classes);
    }

    @Test
    void semDependenciasCiclicasEntrePacotes() {
        ArchRule regra = com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices()
                .matching("com.projetocore.payment.(*)..")
                .should().beFreeOfCycles();
        regra.check(classes);
    }
}
