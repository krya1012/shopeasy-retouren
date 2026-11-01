<script setup>
import { Head, Link, router } from '@inertiajs/vue3';
import { ref } from 'vue';
import {
    index,
    updateStatus,
} from '@/actions/App/Http/Controllers/ReturnController';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
    formatDate,
    formatMoney,
    label,
    outcomeLabels,
    outcomeVariant,
    reasonLabels,
    statusLabels,
    statusVariant,
} from '@/lib/returns';

const props = defineProps({
    return: { type: Object, required: true },
    order: { type: [Object, null], required: true },
    events: { type: Array, required: true },
    canChangeStatus: { type: Boolean, required: true },
    nextStatuses: { type: Array, required: true },
});

defineOptions({
    layout: { breadcrumbs: [{ title: 'Retouren', href: index() }] },
});

const selected = ref('');

function changeStatus() {
    if (!selected.value) return;
    // currentStatus = what this page shows. The REST mock refuses the change (409) if the return moved on meanwhile.
    router.post(
        updateStatus.url(props.return.returnId),
        { status: selected.value, currentStatus: props.return.status },
        { preserveScroll: true, onSuccess: () => (selected.value = '') },
    );
}
</script>

<template>
    <Head :title="`Retoure ${props.return.returnId}`" />

    <div class="flex flex-col gap-4 p-4">
        <div class="flex flex-wrap items-center gap-3">
            <h1 class="text-2xl font-semibold">{{ props.return.returnId }}</h1>
            <Badge
                :variant="statusVariant(props.return.status)"
                data-test="current-status"
            >
                {{ label(statusLabels, props.return.status) }}
            </Badge>
            <Link
                :href="index()"
                class="ml-auto text-sm underline-offset-4 hover:underline"
                >Zurück zur Liste</Link
            >
        </div>

        <div class="grid gap-4 md:grid-cols-2">
            <Card>
                <CardHeader><CardTitle>Details</CardTitle></CardHeader>
                <CardContent
                    class="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm"
                >
                    <span class="text-muted-foreground">Bestellung</span
                    ><span>{{ props.return.orderId }}</span>
                    <span class="text-muted-foreground">Marktplatz</span
                    ><span>{{ props.return.marketplace }}</span>
                    <span class="text-muted-foreground">Grund</span>
                    <span>
                        {{ label(reasonLabels, props.return.reason) }}
                        <template v-if="props.return.reasonText">
                            – „{{ props.return.reasonText }}“</template
                        >
                    </span>
                    <span class="text-muted-foreground">Gemeldet am</span>
                    <span>{{ formatDate(props.return.requestedAt) }}</span>
                    <span class="text-muted-foreground">Zuletzt geändert</span>
                    <span>{{ formatDate(props.return.updatedAt) }}</span>
                    <span class="text-muted-foreground">Kunde</span
                    ><span>{{ props.return.customer.email }}</span>
                    <template v-if="props.return.returnLabelUrl">
                        <span class="text-muted-foreground">Retourenlabel</span>
                        <a
                            :href="props.return.returnLabelUrl"
                            class="underline underline-offset-4"
                            target="_blank"
                            rel="noopener"
                        >
                            Label öffnen
                        </a>
                    </template>
                </CardContent>
            </Card>

            <Card>
                <CardHeader><CardTitle>Artikel</CardTitle></CardHeader>
                <CardContent class="flex flex-col gap-2 text-sm">
                    <div
                        v-for="item in props.return.items"
                        :key="item.sku"
                        class="flex justify-between gap-4"
                    >
                        <span
                            >{{ item.quantity }} × {{ item.productName }}
                            <span class="text-muted-foreground"
                                >({{ item.sku }})</span
                            ></span
                        >
                        <span>{{
                            formatMoney(
                                item.unitPrice * item.quantity,
                                item.currency,
                            )
                        }}</span>
                    </div>
                    <div
                        v-if="order"
                        class="mt-2 flex justify-between border-t pt-2 font-medium"
                    >
                        <span>Bestellwert</span
                        ><span>{{
                            formatMoney(order.totalAmount, order.currency)
                        }}</span>
                    </div>
                </CardContent>
            </Card>
        </div>

        <Card v-if="canChangeStatus">
            <CardHeader><CardTitle>Status ändern</CardTitle></CardHeader>
            <CardContent>
                <p
                    v-if="nextStatuses.length === 0"
                    class="text-sm text-muted-foreground"
                >
                    Von diesem Status aus ist kein weiterer Wechsel möglich.
                </p>
                <form
                    v-else
                    class="flex flex-wrap items-end gap-3"
                    @submit.prevent="changeStatus"
                >
                    <label class="flex flex-col gap-1 text-sm">
                        Neuer Status
                        <select
                            v-model="selected"
                            class="rounded-md border bg-background px-3 py-2"
                            data-test="next-status"
                        >
                            <option value="" disabled>Bitte wählen</option>
                            <option
                                v-for="s in nextStatuses"
                                :key="s"
                                :value="s"
                            >
                                {{ label(statusLabels, s) }}
                            </option>
                        </select>
                    </label>
                    <Button
                        type="submit"
                        :disabled="!selected"
                        data-test="change-status"
                        >Status ändern</Button
                    >
                </form>
            </CardContent>
        </Card>

        <Card>
            <CardHeader><CardTitle>Verlauf</CardTitle></CardHeader>
            <CardContent>
                <p
                    v-if="events.length === 0"
                    class="text-sm text-muted-foreground"
                >
                    Noch keine Statusänderungen erfasst.
                </p>
                <ul v-else class="flex flex-col gap-2 text-sm">
                    <li
                        v-for="e in events"
                        :key="e.id"
                        class="flex flex-wrap items-center gap-2"
                    >
                        <span class="text-muted-foreground">{{
                            formatDate(e.occurred_at ?? e.handled_at)
                        }}</span>
                        <span
                            >{{ label(statusLabels, e.old_status) }} →
                            {{ label(statusLabels, e.new_status) }}</span
                        >
                        <Badge :variant="outcomeVariant(e.outcome)">{{
                            outcomeLabels[e.outcome]
                        }}</Badge>
                    </li>
                </ul>
            </CardContent>
        </Card>
    </div>
</template>
