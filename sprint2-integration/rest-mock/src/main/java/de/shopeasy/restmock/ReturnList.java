package de.shopeasy.restmock;

import java.util.List;

/**
 * Answer of the list call. As on slide 9 of the task, the reports are in the field {@code data}
 * (an "envelope"). Jackson turns this record into {@code {"data":[...]}}.
 */
public record ReturnList(List<ReturnReport> data) {
}
