package de.shopeasy.restmock;

/** Body of {@code PATCH /api/v1/returns/{returnId}}, e.g. {@code {"status":"APPROVED"}}. */
public record StatusUpdate(ReturnStatus status) {
}
