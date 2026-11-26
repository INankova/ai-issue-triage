package io.github.inankova.triage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Temporary: proves that a failing test turns the pull request red (ROADMAP P0.15). Reverted right after. */
class DeliberatelyFailingTests {

    @Test
    void failsOnPurpose() {
        assertThat(1 + 1).isEqualTo(3);
    }
}
