package de.shopeasy.workflow.workers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;

/**
 * Job type {@code assess-return} (step "Classify reason and compute fraud score", {@code t_ai}).
 *
 * <p><b>Stub for Sprint 4</b> with fixed test values: the real AI agent (n8n) follows in Sprint 5 and keeps the same
 * output variables. The reason category comes from the reason code; the fraud score is 0.1, or 0.85 for
 * {@code suspect@example.com}. For {@code ai-down@example.com} the assessment "fails": the worker completes the job
 * with {@code fraudScore = null} (no score), and the rules send the case to a person instead of deciding without a score.
 */
@Component
public class AssessReturnWorker {

    @JobWorker(type = "assess-return")
    public Map<String, Object> handle(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String email = Vars.email(vars);
        Double score;
        if (email.equals(Simulations.AI_DOWN)) {
            score = null;
        } else if (email.equals(Simulations.SUSPECT)) {
            score = Simulations.SUSPECT_SCORE;
        } else {
            score = Simulations.NORMAL_SCORE;
        }
        Map<String, Object> out = new HashMap<>();
        out.put("reasonCategory", category(Vars.text(vars, "reason")));
        out.put("fraudScore", score);
        return out;
    }

    static String category(String reason) {
        if (reason == null) {
            return "UNKNOWN";
        }
        return switch (reason) {
            case "SIZE_TOO_SMALL", "SIZE_TOO_LARGE" -> "SIZE";
            case "NOT_AS_DESCRIBED" -> "EXPECTATION";
            case "DEFECTIVE" -> "QUALITY";
            case "WRONG_ITEM" -> "SHIPPING_ERROR";
            case "CHANGED_MIND" -> "CHANGED_MIND";
            default -> "OTHER";
        };
    }
}
