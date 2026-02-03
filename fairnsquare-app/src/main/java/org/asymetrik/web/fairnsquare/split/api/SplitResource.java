package org.asymetrik.web.fairnsquare.split.api;

import java.net.URI;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.asymetrik.web.fairnsquare.expense.api.dto.ExpenseDTO;
import org.asymetrik.web.fairnsquare.expense.api.mapper.ExpenseMapper;
import org.asymetrik.web.fairnsquare.split.api.dto.ParticipantDTO;
import org.asymetrik.web.fairnsquare.split.api.dto.SplitResponseDTO;
import org.asymetrik.web.fairnsquare.split.api.mapper.ParticipantMapper;
import org.asymetrik.web.fairnsquare.split.api.mapper.SplitMapper;
import org.asymetrik.web.fairnsquare.split.domain.AddExpenseRequest;
import org.asymetrik.web.fairnsquare.split.domain.AddParticipantRequest;
import org.asymetrik.web.fairnsquare.split.domain.AddTypedExpenseRequest;
import org.asymetrik.web.fairnsquare.split.domain.CreateSplitRequest;
import org.asymetrik.web.fairnsquare.split.domain.InvalidParticipantIdError;
import org.asymetrik.web.fairnsquare.split.domain.InvalidSplitIdError;
import org.asymetrik.web.fairnsquare.split.domain.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.SplitNotFoundError;
import org.asymetrik.web.fairnsquare.split.domain.UpdateParticipantRequest;
import org.asymetrik.web.fairnsquare.split.service.SplitUseCases;

/**
 * REST resource for managing splits.
 */
@Path("/api/splits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SplitResource {

    private final SplitUseCases splitService;
    private final SplitMapper splitMapper;
    private final ParticipantMapper participantMapper;
    private final ExpenseMapper expenseMapper;

    @Inject
    public SplitResource(SplitUseCases splitService, SplitMapper splitMapper, ParticipantMapper participantMapper,
            ExpenseMapper expenseMapper) {
        this.splitService = splitService;
        this.splitMapper = splitMapper;
        this.participantMapper = participantMapper;
        this.expenseMapper = expenseMapper;
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

        return Response.created(location).entity(splitMapper.toDTO(split)).build();
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

        return splitService.getSplit(splitId).map(split -> Response.ok(splitMapper.toDTO(split)).build())
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
                .map(participant -> Response.status(Response.Status.CREATED)
                        .entity(participantMapper.toDTO(participant)).build())
                .orElseThrow(() -> new SplitNotFoundError(splitId));
    }

    /**
     * Updates an existing participant in a split.
     *
     * @param splitId
     *            the split identifier
     * @param participantId
     *            the participant identifier
     * @param request
     *            the update participant request
     *
     * @return 200 OK with the updated participant, or 404 Not Found, or 400 Bad Request
     */
    @PUT
    @Path("/{splitId}/participants/{participantId}")
    public Response updateParticipant(@PathParam("splitId") String splitId,
            @PathParam("participantId") String participantId, @Valid UpdateParticipantRequest request) {
        if (!Split.Id.isValid(splitId)) {
            throw new InvalidSplitIdError(splitId);
        }
        if (!Participant.Id.isValid(participantId)) {
            throw new InvalidParticipantIdError(participantId);
        }

        return splitService.updateParticipant(splitId, participantId, request)
                .map(participant -> Response.ok(participantMapper.toDTO(participant)).build())
                .orElseThrow(() -> new SplitNotFoundError(splitId));
    }

    /**
     * Removes a participant from a split.
     *
     * @param splitId
     *            the split identifier
     * @param participantId
     *            the participant identifier
     *
     * @return 204 No Content on success, or 404 Not Found, or 400 Bad Request, or 409 Conflict if participant has
     *         expenses
     */
    @DELETE
    @Path("/{splitId}/participants/{participantId}")
    public Response deleteParticipant(@PathParam("splitId") String splitId,
            @PathParam("participantId") String participantId) {
        if (!Split.Id.isValid(splitId)) {
            throw new InvalidSplitIdError(splitId);
        }
        if (!Participant.Id.isValid(participantId)) {
            throw new InvalidParticipantIdError(participantId);
        }

        boolean removed = splitService.removeParticipant(splitId, participantId);
        if (!removed) {
            throw new SplitNotFoundError(splitId);
        }

        return Response.noContent().build();
    }

    /**
     * Adds an expense to a split.
     *
     * @param splitId
     *            the split identifier
     * @param request
     *            the add expense request
     *
     * @return 201 Created with the created expense, or 404 Not Found, or 400 Bad Request
     *
     * @deprecated Use POST /expenses/by-night or POST /expenses/equal instead.
     */
    @Deprecated
    @POST
    @Path("/{splitId}/expenses")
    public Response addExpense(@PathParam("splitId") String splitId, @Valid AddExpenseRequest request) {
        if (!Split.Id.isValid(splitId)) {
            throw new InvalidSplitIdError(splitId);
        }

        return splitService.addExpense(splitId, request).flatMap(expense -> splitService.getSplit(splitId).map(
                split -> Response.status(Response.Status.CREATED).entity(expenseMapper.toDTO(expense, split)).build()))
                .orElseThrow(() -> new SplitNotFoundError(splitId));
    }

    /**
     * Adds a BY_NIGHT expense to a split. Shares are calculated proportionally to participant nights.
     *
     * @param splitId
     *            the split identifier
     * @param request
     *            the add expense request (splitMode not required, BY_NIGHT is used)
     *
     * @return 201 Created with the created expense, or 404 Not Found, or 400 Bad Request
     */
    @Operation(summary = "Add BY_NIGHT expense", description = "Creates an expense with shares calculated proportionally to participant nights")
    @APIResponse(responseCode = "201", description = "Expense created successfully")
    @APIResponse(responseCode = "404", description = "Split not found")
    @APIResponse(responseCode = "400", description = "Invalid request")
    @POST
    @Path("/{splitId}/expenses/by-night")
    public Response addExpenseByNight(@PathParam("splitId") String splitId, @Valid AddTypedExpenseRequest request) {
        if (!Split.Id.isValid(splitId)) {
            throw new InvalidSplitIdError(splitId);
        }

        return splitService.addExpenseByNight(splitId, request.amount(), request.description(), request.payerId())
                .flatMap(expense -> splitService.getSplit(splitId)
                        .map(split -> Response.status(Response.Status.CREATED)
                                .entity(expenseMapper.toDTO(expense, split)).build()))
                .orElseThrow(() -> new SplitNotFoundError(splitId));
    }

    /**
     * Adds an EQUAL expense to a split. Shares are calculated equally among all participants.
     *
     * @param splitId
     *            the split identifier
     * @param request
     *            the add expense request (splitMode not required, EQUAL is used)
     *
     * @return 201 Created with the created expense, or 404 Not Found, or 400 Bad Request
     */
    @Operation(summary = "Add EQUAL expense", description = "Creates an expense with shares calculated equally among all participants")
    @APIResponse(responseCode = "201", description = "Expense created successfully")
    @APIResponse(responseCode = "404", description = "Split not found")
    @APIResponse(responseCode = "400", description = "Invalid request")
    @POST
    @Path("/{splitId}/expenses/equal")
    public Response addExpenseEqual(@PathParam("splitId") String splitId, @Valid AddTypedExpenseRequest request) {
        if (!Split.Id.isValid(splitId)) {
            throw new InvalidSplitIdError(splitId);
        }

        return splitService.addExpenseEqual(splitId, request.amount(), request.description(), request.payerId())
                .flatMap(expense -> splitService.getSplit(splitId)
                        .map(split -> Response.status(Response.Status.CREATED)
                                .entity(expenseMapper.toDTO(expense, split)).build()))
                .orElseThrow(() -> new SplitNotFoundError(splitId));
    }
}
