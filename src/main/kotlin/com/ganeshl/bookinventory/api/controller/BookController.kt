package com.ganeshl.bookinventory.api.controller

import com.ganeshl.bookinventory.api.dto.BookCreateDTO
import com.ganeshl.bookinventory.api.dto.BookResponseDTO
import com.ganeshl.bookinventory.api.dto.BookUpdateDTO
import com.ganeshl.bookinventory.api.dto.toResponseDTO
import com.ganeshl.bookinventory.api.exception.BookNotFoundException
import com.ganeshl.bookinventory.service.BookService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Book Inventory", description = "APIs for managing book catalog and inventory")
class BookController(private val bookService: BookService) {

    @Operation(summary = "Add a new book to the inventory")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Book created successfully",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDTO::class))]),
        ApiResponse(responseCode = "400", description = "Invalid input data"),
        ApiResponse(responseCode = "409", description = "Book with this ISBN already exists")
    ])
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun addBook(@Valid @RequestBody bookCreateDTO: BookCreateDTO): ResponseEntity<BookResponseDTO> {
        val book = bookService.createBook(bookCreateDTO)
        return ResponseEntity(book.toResponseDTO(), HttpStatus.CREATED)
    }

    @Operation(summary = "Get all books")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "List of all books",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDTO::class))])
    ])
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    fun getAllBooks(): ResponseEntity<List<BookResponseDTO>> {
        val books = bookService.getAllBooks().map { it.toResponseDTO() }
        return ResponseEntity.ok(books)
    }

    @Operation(summary = "Get a book by its ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Book found",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDTO::class))]),
        ApiResponse(responseCode = "404", description = "Book not found")
    ])
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    fun getBookById(@Parameter(description = "ID of the book to be fetched") @PathVariable id: Long): ResponseEntity<BookResponseDTO> {
        val book = bookService.getBookById(id) ?: throw BookNotFoundException("Book with id $id not found")
        return ResponseEntity.ok(book.toResponseDTO())
    }

    @Operation(summary = "Get a book by its ISBN")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Book found",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDTO::class))]),
        ApiResponse(responseCode = "404", description = "Book not found")
    ])
    @GetMapping("/isbn/{isbn}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    fun getBookByIsbn(@Parameter(description = "ISBN of the book to be fetched") @PathVariable isbn: String): ResponseEntity<BookResponseDTO> {
        val book = bookService.findByIsbn(isbn) ?: throw BookNotFoundException("Book with ISBN $isbn not found")
        return ResponseEntity.ok(book.toResponseDTO())
    }

    @Operation(summary = "Update an existing book's details (metadata or quantity)")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Book updated successfully",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDTO::class))]),
        ApiResponse(responseCode = "400", description = "Invalid input data"),
        ApiResponse(responseCode = "404", description = "Book not found")
    ])
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateBook(
        @Parameter(description = "ID of the book to be updated") @PathVariable id: Long,
        @Valid @RequestBody bookUpdateDTO: BookUpdateDTO
    ): ResponseEntity<BookResponseDTO> {
        val updatedBook = bookService.updateBook(id, bookUpdateDTO)
        return ResponseEntity.ok(updatedBook.toResponseDTO())
    }

    @Operation(summary = "Update inventory for a specific book")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Inventory updated successfully",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDTO::class))]),
        ApiResponse(responseCode = "400", description = "Invalid quantity change (e.g., results in negative stock)"),
        ApiResponse(responseCode = "404", description = "Book not found")
    ])
    @PatchMapping("/{id}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateInventory(
        @Parameter(description = "ID of the book whose inventory is to be updated") @PathVariable id: Long,
        @Parameter(description = "Change in quantity (positive to add, negative to remove)") @RequestParam quantityChange: Int
    ): ResponseEntity<BookResponseDTO> {
        val updatedBook = bookService.updateInventory(id, quantityChange)
        return ResponseEntity.ok(updatedBook.toResponseDTO())
    }


    @Operation(summary = "Delete a book by its ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Book deleted successfully"),
        ApiResponse(responseCode = "404", description = "Book not found")
    ])
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteBook(@Parameter(description = "ID of the book to be deleted") @PathVariable id: Long): ResponseEntity<Void> {
        bookService.deleteBook(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Search for books based on criteria (title, author, genre, ISBN)")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "List of books matching criteria",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDTO::class))])
    ])
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    fun searchBooks(
        @Parameter(description = "Part of the book title") @RequestParam(required = false) title: String?,
        @Parameter(description = "Part of the author's name") @RequestParam(required = false) author: String?,
        @Parameter(description = "Book genre") @RequestParam(required = false) genre: String?,
        @Parameter(description = "Exact ISBN of the book") @RequestParam(required = false) isbn: String?
    ): ResponseEntity<List<BookResponseDTO>> {
        val books = bookService.searchBooks(title, author, genre, isbn).map { it.toResponseDTO() }
        return ResponseEntity.ok(books)
    }
}