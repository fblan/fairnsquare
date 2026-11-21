// Open: CDI and JSON-B reach the application's classes by reflection.
open module org.asymetrik.web.fairnsquare {
    requires static java.compiler;

    requires io.vidocq.runtime.core;
    requires io.vidocq.runtime.spi;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires jakarta.interceptor;
    requires jakarta.ws.rs;
    requires jakarta.json;
    requires jakarta.json.bind;
    requires org.eclipse.microprofile.config;

    // The generated REST adapters and routes import the cassini SPI.
    requires io.vidocq.runtime.extensions.jakartaee.core.cassini;
    requires io.vidocq.cassini.api;
    // The pages: vidocq's static files, with index.html for the frontend's own URLs (FrontendMount).
    requires io.vidocq.runtime.extensions.essentials.chappe;
    requires io.vidocq.chappe.api;

    // The CAPTCHA image.
    requires java.desktop;
    requires jnanoid;

    // @Module on the modules' package-info. An automatic module, named after its jar.
    requires static api;

    provides io.vidocq.runtime.extensions.essentials.chappe.spi.MountHandlerProvider
            with org.asymetrik.web.fairnsquare.infrastructure.web.FrontendMount;
}
