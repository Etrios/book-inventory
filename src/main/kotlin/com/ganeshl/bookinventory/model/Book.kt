package com.ganeshl.bookinventory.model

import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
data class Book(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @field:NotBlank(message = "Title cannot be blank")
    @field:Size(max = 255, message = "Title cannot exceed 255 characters")
    var title: String,

    @field:NotBlank(message = "Author cannot be blank")
    @field:Size(max = 100, message = "Author cannot exceed 100 characters")
    var author: String,

    @field:NotBlank(message = "Genre cannot be blank")
    @field:Size(max = 50, message = "Genre cannot exceed 50 characters")
    var genre: String,

    @Column(unique = true)
    @field:NotBlank(message = "ISBN cannot be blank")
    @field:Size(min = 10, max = 13, message = "ISBN must be between 10 and 13 characters")
    var isbn: String,

    @field:NotNull(message = "Price cannot be null")
    @field:Min(value = 0, message = "Price cannot be negative")
    var price: Double,

    @field:NotNull(message = "Quantity cannot be null")
    @field:Min(value = 0, message = "Quantity cannot be negative")
    var quantity: Int
)