package de.shopeasy.restmock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order data for a return.
 *
 * <p><b>Spring Boot basics:</b> {@code @RestController} marks a class whose methods answer HTTP requests; the
 * return values are converted to JSON automatically. {@code @RequestMapping} sets the common URL start and
 * {@code @GetMapping} the URL of one method. {@code @PathVariable} takes a part of the URL (here the order id).
 * The {@link ReturnStore} is passed in through the constructor (dependency injection), Spring creates it once.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final ReturnStore store;

    public OrderController(ReturnStore store) {
        this.store = store;
    }

    /** {@code GET /api/v1/orders/{orderId}}: the order, or 404 if it is unknown. */
    @GetMapping("/{orderId}")
    public Order get(@PathVariable String orderId) {
        return store.findOrder(orderId).orElseThrow(() -> ReturnController.notFound("Order", orderId));
    }
}
