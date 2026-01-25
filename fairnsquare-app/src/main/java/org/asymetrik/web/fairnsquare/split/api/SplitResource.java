package org.asymetrik.web.fairnsquare.split.api;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.asymetrik.web.fairnsquare.split.domain.AddParticipantRequest;
import org.asymetrik.web.fairnsquare.split.domain.CreateSplitRequest;
import org.asymetrik.web.fairnsquare.split.domain.InvalidSplitIdError;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.SplitNotFoundError;
import org.asymetrik.web.fairnsquare.split.service.SplitService;

/**
 * REST resource for managing splits.
 */
@Path("/api/splits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SplitResource {

    private final SplitService splitService;

    @Inject
    public SplitResource(SplitService splitService) {
        this.splitService = splitService;
    }

    /**
     * Creates a new split.
     *
     * @param request
     *            the create split request
     *
     * @return 201 Created with the created split and Location header
     */
    @POST
    public Response createSplit(@Valid CreateSplitRequest request) {
        Split split = splitService.createSplit(request);

        URI location = URI.create("/api/splits/" + split.getId().value());

        return Response.created(location).entity(split).build();
    }

    /**
     * Retrieves a split by ID.
     *
     * @param splitId
     *            the split identifier
     *
     * @return 200 OK with the split, or 404 Not Found, or 400 Bad Request for invalid ID
     */
    @GET
    @Path("/{splitId}")
    public Response getSplit(@PathParam("splitId") String splitId) {
        if (!Split.Id.isValid(splitId)) {
            throw new InvalidSplitIdError(splitId);
        }

        return splitService.getSplit(splitId).map(split -> Response.ok(split).build())
                .orElseThrow(() -> new SplitNotFoundError(splitId));
    }

    /**
     * Adds a participant to a split.
     *
     * @param splitId
     *            the split identifier
     * @param request
     *            the add participant request
     *
     * @return 201 Created with the created participant, or 404 Not Found, or 400 Bad Request
     */
    @POST
    @Path("/{splitId}/participants")
    public Response addParticipant(@PathParam("splitId") String splitId, @Valid AddParticipantRequest request) {
        if (!Split.Id.isValid(splitId)) {
            throw new InvalidSplitIdError(splitId);
        }

        return splitService.addParticipant(splitId, request)
                .map(participant -> Response.status(Response.Status.CREATED).entity(participant).build())
                .orElseThrow(() -> new SplitNotFoundError(splitId));
    }
}
