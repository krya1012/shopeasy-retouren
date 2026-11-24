package de.shopeasy.restmock;

import java.net.URI;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Return reports: list with filter, single call, creation (web shop / e-mail channel) and status change.
 *
 * <p><b>Spring Boot basics:</b>
 * <ul>
 *   <li>{@code @RestController} + {@code @RequestMapping("/api/v1/returns")}: all methods answer URLs that start
 *       with this path. The version {@code v1} in the path allows incompatible changes later as {@code v2}.</li>
 *   <li>{@code @GetMapping} / {@code @PatchMapping}: the HTTP method of the call (read / partial change).</li>
 *   <li>{@code @RequestParam Optional<...>}: a URL parameter like {@code ?status=NEW}. Spring converts the text
 *       to the enum; an unknown value gives HTTP 400. {@code Optional} means the parameter may be missing.</li>
 *   <li>{@code @RequestBody}: the JSON body of the request is converted to a Java object.</li>
 *   <li>{@link ResponseStatusException}: throwing it makes Spring send that HTTP status. With
 *       {@code spring.mvc.problemdetails.enabled=true} the answer has the standard error format
 *       "ProblemDetail" (RFC 9457): {@code {"title":..., "status":404, "detail":...}}.</li>
 * </ul>
 * Status codes used: 200 ok, 201 created, 400 bad request, 404 not found, 409 conflict (status change not allowed).
 */
@RestController
@RequestMapping("/api/v1/returns")
public class ReturnController {

    private final ReturnStore store;

    public ReturnController(ReturnStore store) {
        this.store = store;
    }

    /** {@code GET /api/v1/returns?status=NEW&marketplace=ZALANDO}: list, both filters are optional. */
    @GetMapping
    public ReturnList list(@RequestParam Optional<ReturnStatus> status,
                           @RequestParam Optional<Marketplace> marketplace) {
        return new ReturnList(store.find(status, marketplace));
    }

    /** {@code GET /api/v1/returns/{returnId}}: one report, or 404. */
    @GetMapping("/{returnId}")
    public ReturnReport get(@PathVariable String returnId) {
        return store.findReturn(returnId).orElseThrow(() -> notFound("Return", returnId));
    }

    /**
     * {@code POST /api/v1/returns}: creates a return for the web shop or e-mail channel (status NEW, no marketplace).
     * Answer 201 with the header {@code Location}. The optional header {@code Idempotency-Key} makes the call safe
     * to repeat: the same key again answers 200 with the first return and creates nothing. 400 if the order, the
     * e-mail or the articles are missing or invalid.
     */
    @PostMapping
    public ResponseEntity<ReturnReport> create(@RequestBody CreateReturn request,
                                               @RequestHeader(name = "Idempotency-Key", required = false) String key) {
        validate(request);
        ReturnStore.Created created = store.create(request, key);
        if (!created.isNew()) {
            return ResponseEntity.ok(created.report());
        }
        return ResponseEntity.created(URI.create("/api/v1/returns/" + created.report().returnId()))
                .body(created.report());
    }

    private static void validate(CreateReturn request) {
        if (request.orderId() == null || request.orderId().isBlank()) {
            throw badRequest("The field 'orderId' is missing.");
        }
        if (request.customerEmail() == null || !request.customerEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw badRequest("The field 'customerEmail' is missing or not an e-mail address.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw badRequest("The field 'items' needs at least one article.");
        }
        for (CreateReturn.Line line : request.items()) {
            if (line == null || line.sku() == null || line.sku().isBlank()) {
                throw badRequest("Every article needs a 'sku'.");
            }
            if (line.quantity() < 1) {
                throw badRequest("The quantity of article " + line.sku() + " must be at least 1.");
            }
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * {@code PATCH /api/v1/returns/{returnId}} with {@code {"status":"APPROVED"}}: change the status.
     * 404 if the return is unknown, 400 if the status is missing or invalid, 409 if the change is not allowed.
     * The same status again is fine (200): a repeated request does no harm. The optional field
     * {@code expectedStatus} gives 409 if the return has a different status than the caller expects.
     */
    @PatchMapping("/{returnId}")
    public ReturnReport updateStatus(@PathVariable String returnId, @RequestBody StatusUpdate update) {
        if (update.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The field 'status' is missing.");
        }
        try {
            return store.updateStatus(returnId, update.status(), update.expectedStatus())
                    .orElseThrow(() -> notFound("Return", returnId));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    static ResponseStatusException notFound(String what, String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " " + id + " not found.");
    }
}
