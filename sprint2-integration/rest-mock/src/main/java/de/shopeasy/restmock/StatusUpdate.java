package de.shopeasy.restmock;

/**
 * Body of {@code PATCH /api/v1/returns/{returnId}}, e.g. {@code {"status":"APPROVED"}}.
 *
 * @param status         the new status (required)
 * @param expectedStatus optional: the status the caller believes the return has now. If the return has a
 *                       different status, the request is rejected with 409 (optimistic concurrency), so a
 *                       caller working from an old event cannot overwrite a newer state.
 */
public record StatusUpdate(ReturnStatus status, ReturnStatus expectedStatus) {
}
