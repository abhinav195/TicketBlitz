package com.ticketblitz.event.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventEventListener {

    private final EventProducer eventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCreated(EventCreatedEvent event) {
        log.info("Transaction committed. Publishing event-created to Kafka.");
        eventProducer.publishEventCreated(event.getEvent());
    }
}
