package org.asymetrik.web.fairnsquare;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.asymetrik.modular.verification.ModuleAnalysis.ClassScope;
import org.asymetrik.modular.verification.ModularVerifier;
import org.asymetrik.modular.verification.ModuleAnalysis;
import org.junit.jupiter.api.Test;

/**
 * The module rules, checked on the compiled classes. The compiler only checks signatures (its scan of method bodies is
 * off, see the pom): this test is the one that sees a type used only inside a method. Every violation is listed at
 * once.
 */
class ModularArchitectureTest {

    @Test
    void moduleBoundariesAreRespected() {
        ModuleAnalysis analysis = ModuleAnalysis.of(ClassScope.WITHOUT_TESTS, List.of("org.asymetrik.web.fairnsquare"));

        assertThat(analysis.violations()).extracting(ModularVerifier.Invalid::formatError).isEmpty();
    }
}
