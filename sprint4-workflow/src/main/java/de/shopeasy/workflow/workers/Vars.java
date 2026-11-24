package de.shopeasy.workflow.workers;

import java.util.List;
import java.util.Map;

/** Small helpers to read process variables (a map of JSON values) without casts everywhere. */
final class Vars {

    private Vars() {
    }

    /** The variable as text, or null if it is missing, null or blank. */
    static String text(Map<String, Object> vars, String name) {
        Object value = vars.get(name);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString();
    }

    /** The variable as text; throws {@link IllegalArgumentException} if it is missing. */
    static String required(Map<String, Object> vars, String name) {
        String value = text(vars, name);
        if (value == null) {
            throw new IllegalArgumentException("The process variable '" + name + "' is missing");
        }
        return value;
    }

    static boolean flag(Map<String, Object> vars, String name, boolean defaultValue) {
        Object value = vars.get(name);
        return value instanceof Boolean b ? b : defaultValue;
    }

    static int whole(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalArgumentException("Not a number: " + value);
    }

    /** The variable as a list of JSON objects (a missing variable is an empty list). */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> objects(Map<String, Object> vars, String name) {
        Object value = vars.get(name);
        return value == null ? List.of() : (List<Map<String, Object>>) value;
    }

    /** The e-mail in lower case (for the simulation triggers), or an empty text. */
    static String email(Map<String, Object> vars) {
        String email = text(vars, "customerEmail");
        return email == null ? "" : email.toLowerCase();
    }
}
