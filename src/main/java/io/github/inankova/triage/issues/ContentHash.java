package io.github.inankova.triage.issues;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * A fingerprint of what the triage model reads: an issue's title and body (DESIGN §7). A new hash means the issue
 * needs to be classified and embedded again (P6.3).
 *
 * <p>Labels, state and comments are left out on purpose. Labels are the ground truth, so letting them change the hash
 * would re-run the model every time a maintainer labels an issue.
 */
final class ContentHash {

    private ContentHash() {}

    /**
     * SHA-256 of the title, a NUL character and the body, as lowercase hex. The NUL keeps the two fields apart, so
     * ("ab", "c") and ("a", "bc") differ. A missing body counts as empty.
     */
    static String of(String title, String body) {
        String text = title + '\0' + (body == null ? "" : body);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime supports SHA-256", e);
        }
    }
}
