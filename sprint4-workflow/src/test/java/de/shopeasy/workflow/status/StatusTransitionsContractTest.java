package de.shopeasy.workflow.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The table of allowed status changes in this module must be the shared contract of Sprint 2
 * ({@code sprint2-integration/contracts/status-transitions.json}), like the tests of the REST mock and the Java
 * consumer. This also means: the status sequences of the model are checked against the same table.
 */
class StatusTransitionsContractTest {

    @Test
    void tableMatchesTheSharedContract() throws Exception {
        Map<String, List<String>> contract = JsonMapper.builder().build().readValue(
                Files.readString(Path.of("../sprint2-integration/contracts/status-transitions.json")),
                new TypeReference<Map<String, List<String>>>() { });

        Map<String, List<String>> mine = new TreeMap<>();
        StatusTransitions.ALLOWED.forEach((from, to) -> mine.put(from.name(),
                to.stream().map(Enum::name).sorted().toList()));
        Map<String, List<String>> expected = new TreeMap<>();
        contract.forEach((from, to) -> expected.put(from, to.stream().sorted().toList()));

        assertThat(mine).isEqualTo(expected);
        assertThat(new TreeSet<>(mine.keySet())).containsExactlyInAnyOrderElementsOf(
                java.util.Arrays.stream(ReturnStatus.values()).map(Enum::name).toList());
    }
}
