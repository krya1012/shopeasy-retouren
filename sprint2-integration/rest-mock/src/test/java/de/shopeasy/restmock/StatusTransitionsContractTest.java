package de.shopeasy.restmock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The allowed status changes exist in several places (this module, the Java consumer, the PHP portal). The file
 * {@code contracts/status-transitions.json} is the shared definition. This test fails if this module's table
 * differs from it, so the copies cannot drift apart unnoticed.
 */
class StatusTransitionsContractTest {

    private static final Path CONTRACT = Path.of("..", "contracts", "status-transitions.json");

    @Test
    void tableMatchesTheSharedContract() {
        Map<String, List<String>> contract = JsonMapper.builder().build()
                .readValue(CONTRACT.toFile(), new TypeReference<Map<String, List<String>>>() { });

        assertEquals(Arrays.stream(ReturnStatus.values()).map(Enum::name).sorted().toList(),
                contract.keySet().stream().sorted().toList(), "the contract must list every status");

        for (ReturnStatus from : ReturnStatus.values()) {
            for (ReturnStatus to : ReturnStatus.values()) {
                assertEquals(contract.get(from.name()).contains(to.name()), StatusTransitions.isAllowed(from, to),
                        from + " -> " + to);
            }
        }
    }
}
