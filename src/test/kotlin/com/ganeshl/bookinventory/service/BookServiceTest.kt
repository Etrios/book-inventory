package com.ganeshl.bookinventory.service

import org.junit.jupiter.api.Assertions.*

import com.ganeshl.bookinventory.api.dto.BookCreateDTO
import com.ganeshl.bookinventory.api.dto.BookUpdateDTO
import com.ganeshl.bookinventory.api.exception.BookNotFoundException
import com.ganeshl.bookinventory.api.exception.DuplicateIsbnException
import com.ganeshl.bookinventory.event.BookAddedEvent
import com.ganeshl.bookinventory.event.BookEvent
import com.ganeshl.bookinventory.event.BookInventoryUpdatedEvent
import com.ganeshl.bookinventory.event.BookTitleUpdatedEvent
import com.ganeshl.bookinventory.model.Book
import com.ganeshl.bookinventory.repository.BookRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional

class BookServiceTest {

    @MockK
    private lateinit var bookRepository: BookRepository

    @MockK
    private lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMockKs
    private lateinit var bookService: BookService

    private val eventSlot = slot<BookEvent>()
    private val eventListCapture = mutableListOf<BookEvent>()


    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        every { eventPublisher.publishEvent(any()) } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `createBook should save and return book when ISBN is unique`() {
        val createDTO = BookCreateDTO("Title", "Author", "Genre", "1234567890", 10.0, 5)
        val book = Book(1L, "Title", "Author", "Genre", "1234567890", 10.0, 5)

        every { bookRepository.findByIsbn(createDTO.isbn) } returns Optional.empty()
        every { bookRepository.save(any<Book>()) } returns book

        val result = bookService.createBook(createDTO)

        assertEquals(book, result)
        verify(exactly = 1) { bookRepository.findByIsbn(createDTO.isbn) }
        verify(exactly = 1) { bookRepository.save(any<Book>()) }
        verify(exactly = 1) { eventPublisher.publishEvent(capture(eventSlot)) } // Verify event was published
        val capturedEvent = eventSlot.captured
        assertTrue(capturedEvent is BookAddedEvent)
        assertEquals(book.title, capturedEvent.book.title)
    }

    @Test
    fun `createBook should throw DuplicateIsbnException when ISBN already exists`() {
        val createDTO = BookCreateDTO("Title", "Author", "Genre", "1234567890", 10.0, 5)
        every { bookRepository.findByIsbn(createDTO.isbn) } returns Optional.of(mockk<Book>())

        assertThrows<DuplicateIsbnException> {
            bookService.createBook(createDTO)
        }
        verify(exactly = 1) { bookRepository.findByIsbn(createDTO.isbn) }
        verify(exactly = 0) { bookRepository.save(any<Book>()) }
    }

    @Test
    fun `createBook should throw DuplicateIsbnException on DataIntegrityViolationException during save`() {
        val createDTO = BookCreateDTO("New Title", "New Author", "Fiction", "9876543210", 20.0, 10)
        every { bookRepository.findByIsbn(createDTO.isbn) } returns Optional.empty()
        every { bookRepository.save(any<Book>()) } throws DataIntegrityViolationException("Simulated DB constraint violation")

        assertThrows<DuplicateIsbnException> {
            bookService.createBook(createDTO)
        }

        verify(exactly = 1) { bookRepository.findByIsbn(createDTO.isbn) }
        verify(exactly = 1) { bookRepository.save(any<Book>()) }
    }

    @Test
    fun `getBookById should give null when book not found`() {
        every { bookRepository.findById(1L) } returns Optional.empty()

        val result = bookService.getBookById(1L)

        assertNull(result)
        verify(exactly = 1) { bookRepository.findById(1L)}
    }

    @Test
    fun `getBookByIsbn should give null when book not found`() {
        every { bookRepository.findByIsbn("1112223334") } returns Optional.empty()

        val result = bookService.findByIsbn("1112223334")

        assertNull(result)
        verify(exactly = 1) { bookRepository.findByIsbn("1112223334") }
    }

    @Test
    fun `getBookByIdOrThrow should return book when found`() {
        val book = Book(1L, "Title", "Author", "Genre", "1234567890", 10.0, 5)
        every { bookRepository.findById(1L) } returns Optional.of(book)

        val result = bookService.getBookByIdOrThrow(1L)

        assertEquals(book, result)
        verify(exactly = 1) { bookRepository.findById(1L) }
    }

    @Test
    fun `getBookByIdOrThrow should throw BookNotFoundException when not found`() {
        every { bookRepository.findById(1L) } returns Optional.empty()

        assertThrows<BookNotFoundException> {
            bookService.getBookByIdOrThrow(1L)
        }
        verify(exactly = 1) { bookRepository.findById(1L) }
    }

    @Test
    fun `updateBook should update and return book`() {
        val existingBook = Book(1L, "Old Title", "Old Author", "Old Genre", "1112223334", 15.0, 10)
        val updateDTO = BookUpdateDTO(title = "New Title", author = "New Author", price = 20.0, genre = "Old Genre", quantity = 5)
        val updatedBook = Book(1L, "New Title", "New Author", "Old Genre", "1112223334", 20.0, 5)

        every { bookRepository.findById(1L) } returns Optional.of(existingBook)
        every { bookRepository.save(any<Book>()) } returns updatedBook // Capture and return the argument

        val result = bookService.updateBook(1L, updateDTO)

        assertEquals("New Title", result.title)
        assertEquals("New Author", result.author)
        assertEquals(20.0, result.price)
        assertEquals(5, result.quantity)
        verify(exactly = 1) { bookRepository.findById(1L) }
        verify(exactly = 1) { bookRepository.save(any<Book>()) }
        verify(exactly = 2) { eventPublisher.publishEvent(capture(eventListCapture)) }

        assertEquals(1, eventListCapture.filterIsInstance<BookTitleUpdatedEvent>().count())
        val bookTitleEvent = eventListCapture.filterIsInstance<BookTitleUpdatedEvent>()[0]
        assertEquals(updatedBook.title, bookTitleEvent.book.title)

        assertEquals(1, eventListCapture.filterIsInstance<BookInventoryUpdatedEvent>().count())
        val bookInventoryEvent = eventListCapture.filterIsInstance<BookInventoryUpdatedEvent>()[0]
        assertEquals(updatedBook.quantity, bookInventoryEvent.newQuantity)
    }

    @Test
    fun `updateBook should throw BookNotFoundException if book does not exist`() {
        val updateDTO = BookUpdateDTO(title = "New Title")
        every { bookRepository.findById(1L) } returns Optional.empty()

        assertThrows<BookNotFoundException> {
            bookService.updateBook(1L, updateDTO)
        }
        verify(exactly = 1) { bookRepository.findById(1L) }
        verify(exactly = 0) { bookRepository.save(any<Book>()) }
    }

    @Test
    fun `updateBook should not publish event if quantity is not changed`() {
        val existingBook = Book(1L, "Old Title", "Old Author", "Old Genre", "1112223334", 15.0, quantity = 10)
        // Update DTO does not change quantity
        val updateDTO = BookUpdateDTO(title = "New Title", author = "New Author", genre = "Old Genre", price = 20.0)
        val updatedBook = Book(1L, "New Title", "New Author", "Old Genre", "1112223334", 20.0, 10) // quantity remains 10

        every { bookRepository.findById(1L) } returns Optional.of(existingBook)
        every { bookRepository.save(any<Book>()) } returns updatedBook

        bookService.updateBook(1L, updateDTO)

        verify(exactly = 0) { eventPublisher.publishEvent(ofType(BookInventoryUpdatedEvent::class)) }
    }

    @Test
    fun `updateBook should not publish event if title is not changed`() {
        val existingBook = Book(1L, "Old Title", "Old Author", "Old Genre", "1112223334", 15.0, quantity = 10)
        // Update DTO does not change quantity
        val updateDTO = BookUpdateDTO(title = "Old Title", author = "New Author", genre = "Old Genre", price = 20.0)
        val updatedBook = Book(1L, "Old Title", "New Author", "Old Genre", "1112223334", 20.0, 10) // quantity remains 10

        every { bookRepository.findById(1L) } returns Optional.of(existingBook)
        every { bookRepository.save(any<Book>()) } returns updatedBook

        bookService.updateBook(1L, updateDTO)

        verify(exactly = 0) { eventPublisher.publishEvent(ofType(BookTitleUpdatedEvent::class)) }
    }

    @Test
    fun `updateInventory should correctly update quantity and publish event`() {
        val book = Book(1L, "Title", "Author", "Genre", "123", 10.0, 5)
        val quantityChange = 3
        val expectedNewQuantity = 8
        val updatedBook = book.copy(quantity = expectedNewQuantity)

        every { bookRepository.findById(1L) } returns Optional.of(book)
        every { bookRepository.save(any<Book>()) } returns updatedBook
        every { eventPublisher.publishEvent(any()) } just runs

        val result = bookService.updateInventory(1L, quantityChange)

        assertEquals(expectedNewQuantity, result.quantity)
        verify(exactly = 1) { bookRepository.findById(1L) }
        verify(exactly = 1) { bookRepository.save(book.copy(quantity = expectedNewQuantity)) }
        verify(exactly = 1) { eventPublisher.publishEvent(capture(eventSlot)) }

        val bookInventoryUpdatedEvent = eventSlot.captured as BookInventoryUpdatedEvent
        assertEquals(5, bookInventoryUpdatedEvent.oldQuantity)
        assertEquals(8, bookInventoryUpdatedEvent.newQuantity)
    }

    @Test
    fun `updateInventory should throw IllegalArgumentException if new quantity is negative`() {
        val book = Book(1L, "Title", "Author", "Genre", "123", 10.0, 2)
        val quantityChange = -5 // This will make quantity -3

        every { bookRepository.findById(1L) } returns Optional.of(book)

        assertThrows<IllegalArgumentException> {
            bookService.updateInventory(1L, quantityChange)
        }
        verify(exactly = 1) { bookRepository.findById(1L) }
        verify(exactly = 0) { bookRepository.save(any<Book>()) }
        verify(exactly = 0) { eventPublisher.publishEvent(any()) }
    }

    @Test
    fun `updateBook should throw IllegalArgumentException if new quantity is negative`() {
        val book = Book(1L, "Title", "Author", "Genre", "123", 10.0, 2)
        val updateBookDTO = BookUpdateDTO("Title", "Author", "Genre", 12.0, -2)

        every { bookRepository.findById(1L) } returns Optional.of(book)

        assertThrows<IllegalArgumentException> {
            bookService.updateBook(1L, updateBookDTO)
        }
        verify(exactly = 1) { bookRepository.findById(1L) }
        verify(exactly = 0) { bookRepository.save(any<Book>()) }
        verify(exactly = 0) { eventPublisher.publishEvent(any()) }
    }

    @Test
    fun `updateBook should throw IllegalArgumentException if new price is negative`() {
        val book = Book(1L, "Title", "Author", "Genre", "123", 10.0, 2)
        val updateBookDTO = BookUpdateDTO("Title", "Author", "Genre", -12.0, 2)

        every { bookRepository.findById(1L) } returns Optional.of(book)

        assertThrows<IllegalArgumentException> {
            bookService.updateBook(1L, updateBookDTO)
        }
        verify(exactly = 1) { bookRepository.findById(1L) }
        verify(exactly = 0) { bookRepository.save(any<Book>()) }
        verify(exactly = 0) { eventPublisher.publishEvent(any()) }
    }


    @Test
    fun `deleteBook should call deleteById when book exists`() {
        every { bookRepository.existsById(1L) } returns true
        every { bookRepository.deleteById(1L) } just runs

        bookService.deleteBook(1L)

        verify(exactly = 1) { bookRepository.existsById(1L) }
        verify(exactly = 1) { bookRepository.deleteById(1L) }
    }

    @Test
    fun `deleteBook should throw BookNotFoundException when book does not exist`() {
        every { bookRepository.existsById(1L) } returns false

        assertThrows<BookNotFoundException> {
            bookService.deleteBook(1L)
        }
        verify(exactly = 1) { bookRepository.existsById(1L) }
        verify(exactly = 0) { bookRepository.deleteById(1L) }
    }

    @Test
    fun `searchBooks should call repository searchBooks`() {
        val books = listOf(Book(1L, "Title", "Author", "Genre", "123", 10.0, 5))
        every { bookRepository.searchBooks("Title", null, null, null) } returns books

        val result = bookService.searchBooks("Title", null, null, null)

        assertEquals(books, result)
        verify(exactly = 1) { bookRepository.searchBooks("Title", null, null, null) }
    }

    @Test
    fun `searchBooks should return all books if all criteria are null or blank`() {
        val allBooks = listOf(
            Book(1L, "Title1", "Author1", "Genre1", "123", 10.0, 5),
            Book(2L, "Title2", "Author2", "Genre2", "456", 12.0, 3)
        )
        every { bookRepository.findAll() } returns allBooks

        val resultNull = bookService.searchBooks(null, null, null, null)
        val resultBlank = bookService.searchBooks(" ", "  ", "", null)


        assertEquals(allBooks, resultNull)
        assertEquals(allBooks, resultBlank)
        verify(exactly = 2) { bookRepository.findAll() } // Called twice
        verify(exactly = 0) { bookRepository.searchBooks(any(), any(), any(), any()) }
    }
}