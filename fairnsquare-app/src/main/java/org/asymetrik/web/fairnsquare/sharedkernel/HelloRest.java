package org.asymetrik.web.fairnsquare.sharedkernel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;

@ApplicationScoped
@ApplicationPath("/api")
public class HelloRest {

    @GET
    public String sayHello() {
        return "Hello from Fair n Square!";
    }

}
