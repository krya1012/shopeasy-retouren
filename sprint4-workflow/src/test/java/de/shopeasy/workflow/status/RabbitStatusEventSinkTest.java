package de.shopeasy.workflow.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import tools.jackson.databind.json.JsonMapper;

/** Tests the sender with a fake {@link RabbitTemplate}: we decide how the "broker" answers. */
class RabbitStatusEventSinkTest {

    private RabbitTemplate rabbit;
    private RabbitStatusEventSink sink;

    @BeforeEach
    void setUp() {
        rabbit = mock(RabbitTemplate.class);
        sink = new RabbitStatusEventSink(rabbit, JsonMapper.builder().build(), 1);
    }

    private static StatusEvent event() {
        return StatusEvent.of("RET-1", ReturnStatus.NEW, ReturnStatus.APPROVED, Instant.parse("2026-11-24T10:00:00Z"));
    }

    private void brokerAnswers(CorrelationData.Confirm confirm) {
        doAnswer(call -> {
            call.<CorrelationData>getArgument(3).getFuture().complete(confirm);
            return null;
        }).when(rabbit).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void sendsThePersistentJsonEventWithRoutingKeyAndMessageId() {
        brokerAnswers(new CorrelationData.Confirm(true, null));
        StatusEvent event = event();

        sink.publish(event);

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("returns.status"), eq("return.status.APPROVED"), sent.capture(),
                any(CorrelationData.class));
        String body = new String(sent.getValue().getBody(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"eventId\":\"" + event.eventId() + "\""), body);
        assertTrue(body.contains("\"eventType\":\"ReturnStatusChanged\""), body);
        assertTrue(body.contains("\"returnId\":\"RET-1\""), body);
        assertTrue(body.contains("\"oldStatus\":\"NEW\""), body);
        assertTrue(body.contains("\"newStatus\":\"APPROVED\""), body);
        assertTrue(body.contains("\"occurredAt\":\"2026-11-24T10:00:00Z\""), body);
        assertEquals(MessageDeliveryMode.PERSISTENT, sent.getValue().getMessageProperties().getDeliveryMode());
        assertEquals(event.eventId().toString(), sent.getValue().getMessageProperties().getMessageId());
    }

    @Test
    void brokerRejectsThenError() {
        brokerAnswers(new CorrelationData.Confirm(false, "no space"));
        EventNotDeliveredException e = assertThrows(EventNotDeliveredException.class, () -> sink.publish(event()));
        assertTrue(e.getMessage().contains("no space"));
    }

    @Test
    void noQueueAcceptsTheMessageThenError() {
        doAnswer(call -> {
            Message message = call.getArgument(2);
            CorrelationData correlation = call.getArgument(3);
            correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "returns.status", "x"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        EventNotDeliveredException e = assertThrows(EventNotDeliveredException.class, () -> sink.publish(event()));
        assertTrue(e.getMessage().contains("No queue"));
    }

    @Test
    void brokerNotReachableThenThreeAttemptsAndError() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbit).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        EventNotDeliveredException e = assertThrows(EventNotDeliveredException.class, () -> sink.publish(event()));
        assertTrue(e.getMessage().contains("3 attempts"));
        verify(rabbit, times(3)).send(any(String.class), any(String.class), any(Message.class),
                any(CorrelationData.class));
    }

    @Test
    void secondAttemptWorksThenNoError() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .doAnswer(call -> {
                    call.<CorrelationData>getArgument(3).getFuture().complete(new CorrelationData.Confirm(true, null));
                    return null;
                })
                .when(rabbit).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        sink.publish(event());

        verify(rabbit, times(2)).send(any(String.class), any(String.class), any(Message.class),
                any(CorrelationData.class));
    }
}
