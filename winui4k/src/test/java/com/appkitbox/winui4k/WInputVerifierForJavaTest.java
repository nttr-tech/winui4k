package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WInputVerifier written the way developers who have used Swing's InputVerifier write Java.
 * Compiling at all (an anonymous class overriding only verify, setInputVerifier / getInputVerifier,
 * setVerifyInputWhenFocusTarget / getVerifyInputWhenFocusTarget) checks the API's shape, and the bodies check reading and writing values.
 * How focus moves are stopped is checked in WInputVerifierTest.
 */
class WInputVerifierForJavaTest {
    @Test
    void aVerifierOverridingOnlyVerifyCanBeSetAndReadAndTheDefaultShouldYieldFocusIsTheResultOfVerify() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            WTextField other = new WTextField();
            assertNull(field.getInputVerifier());
            WInputVerifier verifier = new WInputVerifier() {
                @Override
                public boolean verify(WComponent input) {
                    return !((WTextField) input).getText().isEmpty();
                }
            };
            field.setInputVerifier(verifier);
            assertSame(verifier, field.getInputVerifier());
            assertFalse(verifier.shouldYieldFocus(field, other));
            field.setText("a");
            assertTrue(verifier.shouldYieldFocus(field, other));
            assertTrue(verifier.verifyTarget(other));
            field.setInputVerifier(null);
            assertNull(field.getInputVerifier());
        });
    }

    @Test
    void verifyInputWhenFocusTargetDefaultsToTrueAndCanBeReadAndWrittenWithTheSetterAndGetter() {
        onUiThread(() -> {
            WButton cancel = new WButton("Cancel");
            assertTrue(cancel.getVerifyInputWhenFocusTarget());
            cancel.setVerifyInputWhenFocusTarget(false);
            assertFalse(cancel.getVerifyInputWhenFocusTarget());
        });
    }
}
