package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.options.AriaRole;

import io.quarkus.test.junit.QuarkusTest;

/** Without a CAPTCHA token, creating a split first asks for the human verification. */
@QuarkusTest
class CaptchaTest extends E2eTestBase {

    @Override
    protected boolean withCaptchaToken() {
        return false;
    }

    @Test
    void creatingASplitWithoutTokenAsksForHumanVerification() {
        page.navigate("/");

        field("Split Name").fill("Weekend Trip");
        button("Create Split").click();

        assertThat(dialog("Human Verification")).isVisible();
        assertThat(dialog("Human Verification").getByRole(AriaRole.IMG)).isVisible();
        assertThat(page).hasURL(root.toString());
    }
}
