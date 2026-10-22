package de.shopeasy.restmock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bestelldaten zu einer Retoure. */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final ReturnStore store;

    public OrderController(ReturnStore store) {
        this.store = store;
    }

    @GetMapping("/{orderId}")
    public Order get(@PathVariable String orderId) {
        return store.findOrder(orderId).orElseThrow(() -> ReturnController.notFound("Bestellung", orderId));
    }
}
