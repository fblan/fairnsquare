package org.asymetrik.web.fairnsquare.admin.api.internal;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;

/**
 * Turns Quarkus OIDC on only when its provider is configured ({@code quarkus.oidc.auth-server-url}): without it,
 * Quarkus OIDC refuses to start, and the admin pages use the password.
 */
public class OidcSwitch implements ConfigSourceFactory {

    static final String ISSUER = "quarkus.oidc.auth-server-url";
    static final String ENABLED = "quarkus.oidc.tenant-enabled";
    /**
     * Above the runtime values Quarkus records at build time (0), where the issuer isn't known yet; below
     * application.properties (250) and the environment (300), which can still set the switch.
     */
    static final int ORDINAL = 100;

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        ConfigValue issuer = context.getValue(ISSUER);
        boolean configured = issuer != null && issuer.getValue() != null && !issuer.getValue().isBlank();
        return List.of(
                new PropertiesConfigSource(Map.of(ENABLED, String.valueOf(configured)), "admin-oidc-switch", ORDINAL));
    }
}
