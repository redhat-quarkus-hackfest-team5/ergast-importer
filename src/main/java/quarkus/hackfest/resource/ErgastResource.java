package quarkus.hackfest.resource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.infinispan.client.Remote;
import quarkus.hackfest.datamodel.ergast.MRResult;
import quarkus.hackfest.datamodel.ergast.Race;
import quarkus.hackfest.resource.client.ErgastService;

@Path("/")
public class ErgastResource {
    private Logger log = LoggerFactory.getLogger(ErgastResource.class);

    @Inject
    @Remote("last-gp")
    RemoteCache<String, String> lastGp;

    @Inject
    @Channel("twitter-posts-in")
    Emitter<String> twitterPostsEmmiter;

    @Inject
    @RestClient
    ErgastService ergastService;

    @POST
    public Response process() {
        log.info("Ergast processing started...");
        String lastGpStored = lastGp.get("lastGp");
        String nextGp = getNextGp();
        log.info("Ergast Next Gp {} ", nextGp);

        if (nextGp == null) {
            log.warn("No GP has been ever found!");
            return Response.ok().build();
        }

        if (lastGpStored == null || !lastGpStored.equals(nextGp)) {
            log.info("New next GP found...");
            twitterPostsEmmiter.send(nextGp);
            log.info("Updating Cache...");
            lastGp.put("lastGp", nextGp);
        } else {
            log.info("No new GP found!");
        }
        return Response.ok().build();        
    }

    public String getNextGp() throws DateTimeParseException {
        MRResult result;
        Integer offset = 0;
        Integer total = 0;
        Race nextRace = null;

        l1: do {
            result = ergastService.findGp(offset);
            offset = result.getMRData().getRaceTable().getRaces().size() + offset;
            total = Integer.valueOf(result.getMRData().getTotal());
            for (Race race : result.getMRData().getRaceTable().getRaces()) {
                if (LocalDate.now().isBefore(LocalDate.parse(race.getDate(), 
                    DateTimeFormatter.ISO_LOCAL_DATE))) {
                    nextRace = race;
                } else {
                    break l1;
                }
            }
        } while (offset < total);

        if (nextRace != null) {
            return nextRace.getRaceName();
        }
        return null;
    }
}