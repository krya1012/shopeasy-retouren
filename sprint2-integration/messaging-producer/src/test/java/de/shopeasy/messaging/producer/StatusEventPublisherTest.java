package de.shopeasy.messaging.producer;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.ReturnedMessage;

import tools.jackson.databind.json.JsonMapper;

/** Der Publisher wird mit einem Attrappen-RabbitTemplate getestet, der Broker antwortet, wie wir es vorgeben. */
class StatusEventPublisherTest {

    private RabbitTemplate rabbit;
    private StatusEventPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbit = mock(RabbitTemplate.class);
        publisher = new StatusEventPublisher(rabbit, JsonMapper.builder().build(), 1);
    }

    private void brokerAnswers(CorrelationData.Confirm confirm) {
        doAnswer(call -> {
            call.<CorrelationData>getArgument(3).getFuture().complete(confirm);
            return null;
        }).when(rabbit).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void sendetEventAlsPersistentesJsonMitRoutingKey() {
        brokerAnswers(new CorrelationData.Confirm(true, null));
        StatusEvent event = StatusEvent.of("RET-1", ReturnStatus.NEW, ReturnStatus.APPROVED);

        publisher.publish(event);

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("retouren.status"), eq("retoure.status.APPROVED"), sent.capture(),
                any(CorrelationData.class));
        String body = new String(sent.getValue().getBody(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"eventId\":\"" + event.eventId() + "\""), body);
        assertTrue(body.contains("\"returnId\":\"RET-1\""), body);
        assertTrue(body.contains("\"oldStatus\":\"NEW\""), body);
        assertTrue(body.contains("\"newStatus\":\"APPROVED\""), body);
        assertTrue(body.contains("\"occurredAt\":\""), body);
        assertEquals(MessageDeliveryMode.PERSISTENT, sent.getValue().getMessageProperties().getDeliveryMode());
        assertEquals(event.eventId().toString(), sent.getValue().getMessageProperties().getMessageId());
    }

    @Test
    void brokerLehntAbDannFehler() {
        brokerAnswers(new CorrelationData.Confirm(false, "kein Platz"));
        EventNotDeliveredException e = assertThrows(EventNotDeliveredException.class,
                () -> publisher.publish(StatusEvent.of("RET-1", ReturnStatus.NEW, ReturnStatus.APPROVED)));
        assertTrue(e.getMessage().contains("kein Platz"));
    }

    @Test
    void keineQueueNimmtDieNachrichtAnDannFehler() {
        doAnswer(call -> {
            Message message = call.getArgument(2);
            CorrelationData correlation = call.getArgument(3);
            correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "retouren.status", "x"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        EventNotDeliveredException e = assertThrows(EventNotDeliveredException.class,
                () -> publisher.publish(StatusEvent.of("RET-1", ReturnStatus.NEW, ReturnStatus.APPROVED)));
        assertTrue(e.getMessage().contains("Keine Queue"));
    }

    @Test
    void brokerNichtErreichbarDannDreiVersucheUndFehler() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbit).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        EventNotDeliveredException e = assertThrows(EventNotDeliveredException.class,
                () -> publisher.publish(StatusEvent.of("RET-1", ReturnStatus.NEW, ReturnStatus.APPROVED)));
        assertTrue(e.getMessage().contains("3 Versuchen"));
        verify(rabbit, times(3)).send(any(String.class), any(String.class), any(Message.class),
                any(CorrelationData.class));
    }

    @Test
    void zweiterVersuchKlapptDannKeinFehler() {
        brokerAnswers(new CorrelationData.Confirm(true, null));
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .doAnswer(call -> {
                    call.<CorrelationData>getArgument(3).getFuture().complete(new CorrelationData.Confirm(true, null));
                    return null;
                })
                .when(rabbit).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        publisher.publish(StatusEvent.of("RET-1", ReturnStatus.NEW, ReturnStatus.APPROVED));

        verify(rabbit, times(2)).send(any(String.class), any(String.class), any(Message.class),
                any(CorrelationData.class));
    }
}
