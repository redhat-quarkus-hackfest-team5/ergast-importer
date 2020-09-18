package quarkus.hackfest.resource.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import quarkus.hackfest.datamodel.ergast.MRResult;

@Path("/")
@RegisterRestClient(configKey="ergast-api")
public interface ErgastService {
    @GET
    @Path("/current/next.json")
    @Produces("application/json")
    MRResult findGp(@QueryParam Integer offset);
}
