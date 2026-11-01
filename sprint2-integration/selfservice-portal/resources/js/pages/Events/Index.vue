<script setup lang="ts">
import { Head, router } from '@inertiajs/vue3';
import { onMounted, onUnmounted } from 'vue';
import { index } from '@/actions/App/Http/Controllers/EventLogController';
import { Badge } from '@/components/ui/badge';
import {
    formatDate,
    label,
    outcomeLabels,
    outcomeVariant,
    statusLabels,
} from '@/lib/returns';
import type { StatusEventRow } from '@/types/returns';

defineProps<{
    events: StatusEventRow[];
    counts: Record<string, number>;
}>();

defineOptions({
    layout: { breadcrumbs: [{ title: 'Ereignisprotokoll', href: index() }] },
});

// The consumer runs in a separate process, so the page asks for news every 3 seconds.
let timer: ReturnType<typeof setInterval>;
onMounted(() => {
    timer = setInterval(
        () => router.reload({ only: ['events', 'counts'] }),
        3000,
    );
});
onUnmounted(() => clearInterval(timer));
</script>

<template>
    <Head title="Ereignisprotokoll" />

    <div class="flex flex-col gap-4 p-4">
        <h1 class="text-2xl font-semibold">Ereignisprotokoll</h1>
        <p class="text-sm text-muted-foreground">
            Alle Statusmeldungen aus RabbitMQ, die das Portal verarbeitet hat
            (aktualisiert sich automatisch).
        </p>

        <div class="flex flex-wrap gap-2" data-test="counts">
            <Badge
                v-for="(total, outcome) in counts"
                :key="outcome"
                :variant="outcomeVariant(outcome as StatusEventRow['outcome'])"
            >
                {{
                    outcomeLabels[outcome as StatusEventRow['outcome']] ??
                    outcome
                }}: {{ total }}
            </Badge>
        </div>

        <div class="overflow-x-auto rounded-xl border">
            <table class="w-full text-sm">
                <thead class="bg-muted/50 text-left">
                    <tr>
                        <th class="p-3">Zeit</th>
                        <th class="p-3">Retoure</th>
                        <th class="p-3">Statuswechsel</th>
                        <th class="p-3">Ergebnis</th>
                        <th class="p-3">Grund</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="e in events" :key="e.id" class="border-t">
                        <td class="p-3 whitespace-nowrap">
                            {{ formatDate(e.handled_at) }}
                        </td>
                        <td class="p-3">{{ e.return_id ?? '–' }}</td>
                        <td class="p-3">
                            {{ label(statusLabels, e.old_status) }} →
                            {{ label(statusLabels, e.new_status) }}
                        </td>
                        <td class="p-3">
                            <Badge :variant="outcomeVariant(e.outcome)">{{
                                outcomeLabels[e.outcome]
                            }}</Badge>
                        </td>
                        <td class="p-3 text-muted-foreground">
                            {{ e.reason ?? '' }}
                        </td>
                    </tr>
                    <tr v-if="events.length === 0">
                        <td
                            colspan="5"
                            class="p-6 text-center text-muted-foreground"
                        >
                            Noch keine Ereignisse.
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</template>
