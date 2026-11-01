/** German labels for the UI. The values on the left are the codes of the REST mock. */
export const statusLabels = {
    NEW: 'Neu',
    APPROVED: 'Genehmigt',
    REJECTED: 'Abgelehnt',
    IN_REVIEW: 'In Prüfung',
    LABEL_SENT: 'Retourenlabel versendet',
    RECEIVED: 'Im Lager eingegangen',
    EXPIRED: 'Frist abgelaufen',
    INSPECTED: 'Geprüft',
    ACCEPTED: 'Angenommen',
    MANUAL_REVIEW: 'Manuelle Prüfung',
    REFUNDED: 'Erstattet',
    CLOSED: 'Abgeschlossen',
};

export const reasonLabels = {
    SIZE_TOO_SMALL: 'Zu klein',
    SIZE_TOO_LARGE: 'Zu groß',
    NOT_AS_DESCRIBED: 'Nicht wie beschrieben',
    DEFECTIVE: 'Defekt',
    CHANGED_MIND: 'Gefällt nicht mehr',
    WRONG_ITEM: 'Falscher Artikel',
};

export const outcomeLabels = {
    processed: 'Verarbeitet',
    duplicate: 'Duplikat (ignoriert)',
    rejected: 'Abgelehnt (Fehler-Queue)',
    invalid: 'Ungültig (Fehler-Queue)',
    manual: 'Manuell geändert',
};

export const label = (labels, code) => (code ? (labels[code] ?? code) : '–');

export function statusVariant(status) {
    if (['REJECTED', 'EXPIRED'].includes(status)) return 'destructive';
    if (['ACCEPTED', 'REFUNDED', 'CLOSED'].includes(status)) return 'default';
    if (['MANUAL_REVIEW', 'IN_REVIEW'].includes(status)) return 'outline';
    return 'secondary';
}

export function outcomeVariant(outcome) {
    if (outcome === 'rejected' || outcome === 'invalid') return 'destructive';
    if (outcome === 'processed') return 'default';
    return 'secondary';
}

export const formatDate = (iso) =>
    iso
        ? new Date(iso).toLocaleString('de-DE', {
              dateStyle: 'short',
              timeStyle: 'short',
          })
        : '–';

export const formatMoney = (amount, currency = 'EUR') =>
    new Intl.NumberFormat('de-DE', { style: 'currency', currency }).format(
        amount,
    );
