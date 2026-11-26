package io.github.inankova.triage;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Module boundary rules from ADR-0008, checked on every build. Each top-level package under
 * {@code io.github.inankova.triage} is a module (DESIGN §6.2).
 */
class ArchitectureTests {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.inankova.triage");

    @Test
    void modulesHaveNoDependencyCycles() {
        slices().matching("io.github.inankova.triage.(*)..")
                .should()
                .beFreeOfCycles()
                .check(PRODUCTION_CLASSES);
    }
}
