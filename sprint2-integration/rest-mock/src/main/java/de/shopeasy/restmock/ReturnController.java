package de.shopeasy.restmock;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Retourenmeldungen: Liste mit Filter, Einzelabruf und Statusänderung (unerlaubte Wechsel geben 409). */
@RestController
@RequestMapping("/api/v1/returns")
public class ReturnController {

    private final ReturnStore store;

    public ReturnController(ReturnStore store) {
        this.store = store;
    }

    @GetMapping
    public ReturnList list(@RequestParam Optional<ReturnStatus> status,
                           @RequestParam Optional<Marketplace> marketplace) {
        return new ReturnList(store.find(status, marketplace));
    }

    @GetMapping("/{returnId}")
    public ReturnReport get(@PathVariable String returnId) {
        return store.findReturn(returnId).orElseThrow(() -> notFound("Retoure", returnId));
    }

    @PatchMapping("/{returnId}")
    public ReturnReport updateStatus(@PathVariable String returnId, @RequestBody StatusUpdate update) {
        if (update.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Das Feld 'status' fehlt.");
        }
        try {
            return store.updateStatus(returnId, update.status()).orElseThrow(() -> notFound("Retoure", returnId));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    static ResponseStatusException notFound(String what, String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " " + id + " nicht gefunden.");
    }
}
