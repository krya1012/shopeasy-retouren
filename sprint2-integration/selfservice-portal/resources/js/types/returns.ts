/** Shapes of the data the Laravel controllers send to the pages (they mirror the REST mock). */

export type ReturnSummary = {
    returnId: string;
    orderId: string;
    marketplace: string;
    status: string;
    reason: string | null;
    requestedAt: string | null;
    itemCount: number;
};

export type ReturnItem = {
    sku: string;
    productName: string;
    quantity: number;
    unitPrice: number;
    currency: string;
};

export type ReturnDetail = {
    returnId: string;
    orderId: string;
    marketplace: string;
    status: string;
    reason: string | null;
    reasonText: string | null;
    requestedAt: string | null;
    updatedAt: string | null;
    items: ReturnItem[];
    customer: { customerId: string; email: string };
    returnLabelUrl: string | null;
};

export type OrderDetail = {
    orderId: string;
    totalAmount: number;
    currency: string;
};

/** One row of the event log. Most fields can be null because broken messages are logged too. */
export type StatusEventRow = {
    id: number;
    event_id: string | null;
    return_id: string | null;
    old_status: string | null;
    new_status: string | null;
    occurred_at: string | null;
    outcome: 'processed' | 'duplicate' | 'rejected' | 'invalid' | 'manual';
    reason: string | null;
    handled_at: string;
};
