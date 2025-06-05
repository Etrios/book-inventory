package com.ganeshl.bookinventory.api.controller

import org.junit.jupiter.api.Assertions.*

import com.ganeshl.bookinventory.api.dto.BookCreateDTO
import com.ganeshl.bookinventory.api.dto.BookResponseDTO
import com.ganeshl.bookinventory.api.exception.DuplicateIsbnException
import com.ganeshl.bookinventory.model.Book
import com.ganeshl.bookinventory.service.BookService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import com.ganeshl.bookinventory.config.SecurityConfig // Import your security config
import com.ganeshl.bookinventory.api.dto.BookUpdateDTO
import com.ganeshl.bookinventory.api.exception.BookNotFoundException

@WebMvcTest(BookController::class)
@Import(SecurityConfig::class) // Import security config to apply rules
class BookControllerTest(@Autowired val mockMvc: MockMvc, @Autowired val objectMapper: ObjectMapper) {

    @MockkBean // Use MockkBean for Spring Boot tests with MockK
    private lateinit var bookService: BookService

    private val book1 = Book(1L, "Kotlin in Action", "Dmitry Jemerov", "Programming", "9781617293290", 40.0, 10)
    private val book1ResponseDTO = BookResponseDTO(1L, "Kotlin in Action", "Dmitry Jemerov", "Programming", "9781617293290", 40.0, 10)

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `addBook should return 201 Created when book is created`() {
        val createDTO = BookCreateDTO("New Book", "New Author", "Fiction", "1234509876", 25.0, 5)
        val createdBook = Book(2L, "New Book", "New Author", "Fiction", "1234509876", 25.0, 5)
        val createdBookResponseDTO = BookResponseDTO(2L, "New Book", "New Author", "Fiction", "1234509876", 25.0, 5)

        every { bookService.createBook(createDTO) } returns createdBook

        mockMvc.perform(post("/api/v1/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(createdBookResponseDTO.id))
            .andExpect(jsonPath("$.title").value(createdBookResponseDTO.title))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `addBook should throw validation errors for validation failures`() {
        val createDTO = BookCreateDTO("New Book", "New Author", "Fiction", "1234", 25.0, 5)

        mockMvc.perform(post("/api/v1/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `addBook should return 500 error for an unknown exception`() {
        val createDTO = BookCreateDTO("New Book", "New Author", "Fiction", "1234509876", 25.0, 5)

        every { bookService.createBook(createDTO) } throws RuntimeException("Unknown Error")

        mockMvc.perform(post("/api/v1/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().is5xxServerError)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `addBook should return 409 Conflict when ISBN already exists`() {
        val createDTO = BookCreateDTO("Existing Book", "Author", "Genre", "1112223334", 30.0, 3)

        every { bookService.createBook(createDTO) } throws DuplicateIsbnException("ISBN exists")

        mockMvc.perform(post("/api/v1/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isConflict)
    }

    @Test
    @WithMockUser(username = "user", roles = ["USER"]) // User role cannot add books
    fun `addBook should return 403 Forbidden for non-admin user`() {
        val createDTO = BookCreateDTO("New Book", "New Author", "Fiction", "1234509876", 25.0, 5)
        // No need to mock bookService.createBook as it shouldn't be reached

        mockMvc.perform(post("/api/v1/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isForbidden)
    }


    @Test
    @WithMockUser // Default user role is USER if not specified, or use (username="user", roles=["USER"])
    fun `getAllBooks should return 200 OK with list of books`() {
        every { bookService.getAllBooks() } returns listOf(book1)

        mockMvc.perform(get("/api/v1/books"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].title").value(book1.title))
    }

    @Test
    @WithMockUser(username = "BOT", roles=[""])
    fun `getAllBooks should return unAuthorized for unknown user`() {
        every { bookService.getAllBooks() } returns listOf(book1)

        mockMvc.perform(get("/api/v1/books"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser
    fun `getBookById should return 200 OK when book found`() {
        every { bookService.getBookById(1L) } returns book1

        mockMvc.perform(get("/api/v1/books/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value(book1.title))
    }

    @Test
    @WithMockUser
    fun `getBookById should fail for id other than long`() {

        mockMvc.perform(get("/api/v1/books/5.5"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser
    fun `getBookById should return 404 Not Found when book not found`() {
        every { bookService.getBookById(99L) } returns null // or throws BookNotFoundException depending on controller logic

        mockMvc.perform(get("/api/v1/books/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `getBookByIsbn should return 200 OK when book found`() {
        every { bookService.findByIsbn("1112223334") } returns book1

        mockMvc.perform(get("/api/v1/books/isbn/1112223334"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value(book1.title))
    }

    @Test
    @WithMockUser
    fun `getBookByIsbn should return 404 Not Found when book not found`() {
        every { bookService.findByIsbn("111222333") } returns null

        mockMvc.perform(get("/api/v1/books/isbn/111222333"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `updateBook should return 200 OK when book is updated`() {
        val updateDTO = BookUpdateDTO(title = "Updated Title", quantity = 8)
        val updatedBook = book1.copy(title = "Updated Title", quantity = 8)
        val updatedBookResponseDTO = book1ResponseDTO.copy(title = "Updated Title", quantity = 8)

        every { bookService.updateBook(1L, updateDTO) } returns updatedBook

        mockMvc.perform(put("/api/v1/books/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value(updatedBookResponseDTO.title))
            .andExpect(jsonPath("$.quantity").value(updatedBookResponseDTO.quantity))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `updateBook should return 404 Not Found when book to update is not found`() {
        val updateDTO = BookUpdateDTO(title = "Updated Title")
        every { bookService.updateBook(99L, updateDTO) } throws BookNotFoundException("Book not found")

        mockMvc.perform(put("/api/v1/books/99")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `updateInventory should return 200 OK when inventory updated`() {
        val quantityChange = -2
        val updatedBook = book1.copy(quantity = book1.quantity + quantityChange)
        val updatedBookResponseDTO = book1ResponseDTO.copy(quantity = book1.quantity + quantityChange)

        every { bookService.updateInventory(1L, quantityChange) } returns updatedBook

        mockMvc.perform(patch("/api/v1/books/1/inventory?quantityChange=$quantityChange"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(updatedBookResponseDTO.quantity))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `updateInventory should return 400 Bad Request for invalid quantity`() {
        val quantityChange = -100 // Assuming this makes inventory negative
        every { bookService.updateInventory(1L, quantityChange) } throws IllegalArgumentException("Inventory cannot be negative")

        mockMvc.perform(patch("/api/v1/books/1/inventory?quantityChange=$quantityChange"))
            .andExpect(status().isBadRequest)
    }


    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `deleteBook should return 204 No Content when book is deleted`() {
        justRun { bookService.deleteBook(1L) } // For void methods

        mockMvc.perform(delete("/api/v1/books/1"))
            .andExpect(status().isNoContent)
        verify(exactly = 1) { bookService.deleteBook(1L) }
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `deleteBook should return 404 Not Found when book to delete is not found`() {
        every { bookService.deleteBook(99L) } throws BookNotFoundException("Book not found")

        mockMvc.perform(delete("/api/v1/books/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `searchBooks should return 200 OK with matching books`() {
        val queryTitle = "Kotlin"
        every { bookService.searchBooks(queryTitle, null, null, null) } returns listOf(book1)

        mockMvc.perform(get("/api/v1/books/search?title=$queryTitle"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value(book1.title))
    }

    @Test
    @WithMockUser
    fun `searchBooks should return empty List for non matching criteria`() {
        val queryParams = "?title=Hello &author=test &isbn=432423%20&genre=test "

        every { bookService.searchBooks(any(), any(), any(), any()) } returns emptyList()

        mockMvc.perform(get("/api/v1/books/search$queryParams"))
            .andExpect(status().isOk)
            .andExpect(content().string("[]"))
    }
}