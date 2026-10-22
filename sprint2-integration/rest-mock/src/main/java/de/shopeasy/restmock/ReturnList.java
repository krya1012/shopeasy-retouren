package de.shopeasy.restmock;

import java.util.List;

/** Antwort der Liste: Die Meldungen stehen wie auf Folie 9 der Aufgabe im Feld {@code data}. */
public record ReturnList(List<ReturnReport> data) {
}
