package io.github.inankova.triage.issues;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Expected values were computed independently with Node's crypto module, not with this code. */
class ContentHashTests {

    @Test
    void isTheSha256OfTitleAndBodySeparatedByANulCharacter() {
        assertThat(ContentHash.of("Startup fails", "Steps to reproduce"))
                .isEqualTo("5c936f11c23cf1433b9704b113399ee28fe5e466169f442c67426643b215a166");
    }

    @Test
    void treatsAMissingBodyAsEmpty() {
        assertThat(ContentHash.of("Startup fails", null))
                .isEqualTo(ContentHash.of("Startup fails", ""))
                .isEqualTo("8d1a46569e7a5d8cb5bcbd7c995009f8cd6a438955b92f4bec8371bc8a32a50c");
    }

    @Test
    void keepsTitleAndBodyApart() {
        assertThat(ContentHash.of("ab", "c")).isNotEqualTo(ContentHash.of("a", "bc"));
    }
}
