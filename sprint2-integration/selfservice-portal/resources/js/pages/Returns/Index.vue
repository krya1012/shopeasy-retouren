<script setup>
import { Head, Link, router } from '@inertiajs/vue3';
import { reactive } from 'vue';
import { index, show } from '@/actions/App/Http/Controllers/ReturnController';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import {
    formatDate,
    label,
    reasonLabels,
    statusLabels,
    statusVariant,
} from '@/lib/returns';

const props = defineProps({
    returns: { type: Array, required: true },
    filters: { type: Object, required: true },
    statuses: { type: Array, required: true },
    marketplaces: { type: Array, required: true },
    apiError: { type: [String, null], required: true },
});

defineOptions({
    layout: { breadcrumbs: [{ title: 'Retouren', href: index() }] },
});

const form = reactive({
    status: props.filters.status ?? '',
    marketplace: props.filters.marketplace ?? '',
});

function applyFilters() {
    const query = Object.fromEntries(
        Object.entries(form).filter(([, value]) => value !== ''),
    );
    router.get(index.url(), query, { preserveState: true, replace: true });
}
</script>

<template>
    <Head title="Retouren" />

    <div class="flex flex-col gap-4 p-4">
        <h1 class="text-2xl font-semibold">Retouren</h1>

        <Alert v-if="apiError" variant="destructive">
            <AlertDescription>{{ apiError }}</AlertDescription>
        </Alert>

        <div class="flex flex-wrap gap-3">
            <label class="flex flex-col gap-1 text-sm">
                Status
                <select
                    v-model="form.status"
                    class="rounded-md border bg-background px-3 py-2"
                    data-test="filter-status"
                    @change="applyFilters"
                >
                    <option value="">Alle</option>
                    <option v-for="s in statuses" :key="s" :value="s">
                        {{ label(statusLabels, s) }}
                    </option>
                </select>
            </label>
            <label class="flex flex-col gap-1 text-sm">
                Marktplatz
                <select
                    v-model="form.marketplace"
                    class="rounded-md border bg-background px-3 py-2"
                    data-test="filter-marketplace"
                    @change="applyFilters"
                >
                    <option value="">Alle</option>
                    <option v-for="m in marketplaces" :key="m" :value="m">
                        {{ m }}
                    </option>
                </select>
            </label>
        </div>

        <div class="overflow-x-auto rounded-xl border">
            <table class="w-full text-sm">
                <thead class="bg-muted/50 text-left">
                    <tr>
                        <th class="p-3">Retoure</th>
                        <th class="p-3">Bestellung</th>
                        <th class="p-3">Marktplatz</th>
                        <th class="p-3">Grund</th>
                        <th class="p-3">Artikel</th>
                        <th class="p-3">Gemeldet am</th>
                        <th class="p-3">Status</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="r in returns" :key="r.returnId" class="border-t">
                        <td class="p-3 font-medium">
                            <Link
                                :href="show(r.returnId)"
                                class="underline-offset-4 hover:underline"
                            >
                                {{ r.returnId }}
                            </Link>
                        </td>
                        <td class="p-3">{{ r.orderId }}</td>
                        <td class="p-3">{{ r.marketplace }}</td>
                        <td class="p-3">{{ label(reasonLabels, r.reason) }}</td>
                        <td class="p-3">{{ r.itemCount }}</td>
                        <td class="p-3">{{ formatDate(r.requestedAt) }}</td>
                        <td class="p-3">
                            <Badge :variant="statusVariant(r.status)">{{
                                label(statusLabels, r.status)
                            }}</Badge>
                        </td>
                    </tr>
                    <tr v-if="returns.length === 0 && !apiError">
                        <td
                            colspan="7"
                            class="p-6 text-center text-muted-foreground"
                        >
                            Keine Retouren gefunden.
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</template>
