package com.ganeshl.bookinventory.api.dto

import com.ganeshl.bookinventory.model.Book
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class BookCreateDTO(
    @field:NotBlank(message = "Title cannot be blank")
    @field:Size(max = 255)
    val title: String,

    @field:NotBlank(message = "Author cannot be blank")
    @field:Size(max = 100)
    val author: String,

    @field:NotBlank(message = "Genre cannot be blank")
    @field:Size(max = 50)
    val genre: String,

    @field:NotBlank(message = "ISBN cannot be blank")
    @field:Size(min = 10, max = 13)
    val isbn: String,

    @field:NotNull(message = "Price cannot be null")
    @field:Min(0)
    val price: Double,

    @field:NotNull(message = "Quantity cannot be null")
    @field:Min(0)
    val quantity: Int
)

data class BookUpdateDTO(
    val title: String? = null,
    val author: String? = null,
    val genre: String? = null,
    val price: Double? = null,
    val quantity: Int? = null
)

data class BookResponseDTO(
    val id: Long,
    val title: String,
    val author: String,
    val genre: String,
    val isbn: String,
    val price: Double,
    val quantity: Int
)

fun Book.toResponseDTO(): BookResponseDTO {
    return BookResponseDTO(
        id = this.id!!, // ID is guaranteed to be non-null for an existing book
        title = this.title,
        author = this.author,
        genre = this.genre,
        isbn = this.isbn,
        price = this.price,
        quantity = this.quantity
    )
}