package org.asymetrik.web.fairnsquare.admin;

import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;

/**
 * Admins sign in with an OIDC provider: WireMock's, whose login form signs in alice, in the groups admin and user. Only
 * the admin group's members are admins.
 */
public class AdminOidcProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.oidc.auth-server-url", "${keycloak.url}/realms/quarkus", "quarkus.oidc.client-id",
                "fairnsquare", "quarkus.oidc.credentials.secret", "secret", "admin.oidc.group", "admin");
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(OidcWiremockTestResource.class));
    }
}
