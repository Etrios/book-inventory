package com.ganeshl.bookinventory.service

import com.ganeshl.bookinventory.api.dto.BookCreateDTO
import com.ganeshl.bookinventory.api.dto.BookUpdateDTO
import com.ganeshl.bookinventory.api.exception.BookNotFoundException
import com.ganeshl.bookinventory.api.exception.DuplicateIsbnException
import com.ganeshl.bookinventory.event.BookAddedEvent
import com.ganeshl.bookinventory.event.BookInventoryUpdatedEvent
import com.ganeshl.bookinventory.event.BookTitleUpdatedEvent
import com.ganeshl.bookinventory.model.Book
import com.ganeshl.bookinventory.repository.BookRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
    private val bookRepository: BookRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(BookService::class.java)

    @Transactional
    fun createBook(bookCreateDTO: BookCreateDTO): Book {
        if (bookRepository.findByIsbn(bookCreateDTO.isbn).isPresent) {
            throw DuplicateIsbnException("Book with ISBN ${bookCreateDTO.isbn} already exists.")
        }
        val book = Book(
            title = bookCreateDTO.title,
            author = bookCreateDTO.author,
            genre = bookCreateDTO.genre,
            isbn = bookCreateDTO.isbn,
            price = bookCreateDTO.price,
            quantity = bookCreateDTO.quantity
        )
        try {
            val savedBook = bookRepository.save(book)
            logger.info("Created book: ${savedBook.title} (ISBN: ${savedBook.isbn})")
            eventPublisher.publishEvent(BookAddedEvent(this, savedBook))
            return savedBook
        } catch (e: DataIntegrityViolationException) {
            // This handles potential race conditions if another transaction creates the same ISBN
            // after the initial check but before this save.
            logger.error("Data integrity violation while creating book with ISBN ${bookCreateDTO.isbn}", e)
            throw DuplicateIsbnException("Book with ISBN ${bookCreateDTO.isbn} already exists (concurrent creation).")
        }
    }

    fun getAllBooks(): List<Book> {
        logger.info("Fetching all books")
        return bookRepository.findAll()
    }

    fun getBookById(id: Long): Book? {
        logger.info("Fetching book by id: $id")
        return bookRepository.findByIdOrNull(id)
    }
    fun getBookByIdOrThrow(id: Long): Book {
        logger.info("Fetching book by id: $id")
        return bookRepository.findById(id).orElseThrow {
            logger.warn("Book with id $id not found")
            BookNotFoundException("Book with id $id not found")
        }
    }


    fun findByIsbn(isbn: String): Book? {
        logger.info("Fetching book by ISBN: $isbn")
        return bookRepository.findByIsbn(isbn).orElse(null)
    }

    @Transactional
    fun updateBook(id: Long, bookUpdateDTO: BookUpdateDTO): Book {
        val existingBook = getBookByIdOrThrow(id)

        // Create a copy for existing book, as existing book is used for publishing events.
        val bookToUpdate = existingBook.copy()
        bookUpdateDTO.title?.let { bookToUpdate.title = it }
        bookUpdateDTO.author?.let { bookToUpdate.author = it }
        bookUpdateDTO.genre?.let { bookToUpdate.genre = it }
        bookUpdateDTO.price?.let {
            if (it < 0) throw IllegalArgumentException("Price cannot be negative.")
            bookToUpdate.price = it
        }
        bookUpdateDTO.quantity?.let {
            if (it < 0) throw IllegalArgumentException("Quantity cannot be negative.")
            bookToUpdate.quantity = it
        }

        val updatedBook = bookRepository.save(bookToUpdate)
        logger.info("Updated book with id: $id. New title: ${updatedBook.title}")

        publishEvents(existingBook, updatedBook)

        return updatedBook
    }

    private fun publishEvents(oldBook: Book, updatedBook: Book) {
        if (updatedBook.title.isNotEmpty() && updatedBook.title != oldBook.title) {
            eventPublisher.publishEvent(BookTitleUpdatedEvent(this, updatedBook))
        }
        if (updatedBook.quantity != oldBook.quantity) {
            eventPublisher.publishEvent(BookInventoryUpdatedEvent(this, updatedBook, updatedBook.quantity, oldBook.quantity))
        }
    }

    @Transactional
    fun updateInventory(id: Long, quantityChange: Int): Book {
        val book = getBookByIdOrThrow(id)
        val oldQuantity = book.quantity
        val newQuantity = book.quantity + quantityChange
        if (newQuantity < 0) {
            throw IllegalArgumentException("Inventory level cannot go below zero for book ID $id.")
        }
        book.quantity = newQuantity
        val savedBook =  bookRepository.save(book)
        logger.info("Updated inventory for book id $id. Old quantity: $oldQuantity, New quantity: $newQuantity")
        eventPublisher.publishEvent(BookInventoryUpdatedEvent(this, savedBook, newQuantity, oldQuantity))
        return savedBook
    }


    @Transactional
    fun deleteBook(id: Long) {
        if (!bookRepository.existsById(id)) {
            logger.warn("Attempted to delete non-existent book with id $id")
            throw BookNotFoundException("Book with id $id not found, cannot delete.")
        }
        bookRepository.deleteById(id)
        logger.info("Deleted book with id: $id")
    }

    fun searchBooks(title: String?, author: String?, genre: String?, isbn: String?): List<Book> {
        logger.info("Searching books with criteria - Title: $title, Author: $author, Genre: $genre, ISBN: $isbn")
        if (title.isNullOrBlank() && author.isNullOrBlank() && genre.isNullOrBlank() && isbn.isNullOrBlank()) {
            return getAllBooks() // Return all if no criteria specified
        }
        return bookRepository.searchBooks(
            title?.trim(),
            author?.trim(),
            genre?.trim(),
            isbn?.trim()
        )
    }
}