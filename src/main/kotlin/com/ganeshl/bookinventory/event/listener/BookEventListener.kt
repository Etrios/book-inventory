package com.ganeshl.bookinventory.event.listener

import com.ganeshl.bookinventory.event.BookAddedEvent
import com.ganeshl.bookinventory.event.BookInventoryUpdatedEvent
import com.ganeshl.bookinventory.event.BookTitleUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class BookEventListener {
    private val logger = LoggerFactory.getLogger(BookEventListener::class.java)

    @EventListener
    fun handleBookAddedEvent(event: BookAddedEvent) {
        logger.info("[EVENT] Book added: ID=${event.book.id}, Title='${event.book.title}', ISBN='${event.book.isbn}'")
        // Here you could, for example, send this event to a message queue
        // for other services to consume if this were a microservices architecture.
    }

    @EventListener
    fun handleInventoryUpdatedEvent(event: BookInventoryUpdatedEvent) {
        logger.info("[EVENT] Inventory updated for Book ID=${event.book.id}: Old Quantity=${event.oldQuantity}, New Quantity=${event.newQuantity}")
        // Similarly, this event could be published to a message broker.
    }

    @EventListener
    fun handleBookTitleUpdated(event: BookTitleUpdatedEvent) {
        logger.info("[EVENT] Book Title Updated for Book ID=${event.book.id}: ${event.book.title}")
    }
}