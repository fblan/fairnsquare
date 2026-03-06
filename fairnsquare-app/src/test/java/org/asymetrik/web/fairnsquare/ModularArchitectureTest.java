package org.asymetrik.web.fairnsquare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.asymetrik.modular.verification.ModularVerifier;
import org.junit.jupiter.api.Test;

class ModularArchitectureTest {

    @Test
    void moduleBoundariesAreRespected() {
        ModularVerifier verifier = new ModularVerifier("org.asymetrik.web.fairnsquare");

        ModularVerifier.Result result = verifier.verify();

        if (result instanceof ModularVerifier.Invalid invalid) {
            if (invalid instanceof ModularVerifier.NestedModuleViolation v) {
                fail(v.formatError());
            } else if (invalid instanceof ModularVerifier.ExportViolation v) {
                fail(v.formatError());
            }
        }

        assertThat(result).isInstanceOf(ModularVerifier.Valid.class);
    }
}
