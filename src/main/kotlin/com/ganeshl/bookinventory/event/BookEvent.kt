package com.ganeshl.bookinventory.event

import com.ganeshl.bookinventory.model.Book
import org.springframework.context.ApplicationEvent


abstract class BookEvent(source: Any, val book: Book) : ApplicationEvent(source)
class BookAddedEvent(source: Any, book: Book) : BookEvent(source, book)

class BookInventoryUpdatedEvent(source: Any, book: Book, val newQuantity: Int, val oldQuantity: Int) : BookEvent(source, book)

class BookTitleUpdatedEvent(source: Any, book: Book) : BookEvent(source, book)