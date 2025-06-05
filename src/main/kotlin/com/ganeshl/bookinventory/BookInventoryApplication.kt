package com.ganeshl.bookinventory

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@OpenAPIDefinition(info = Info(title = "Book Inventory Service", version = "1.0", description = "API for managing book inventory"))
class BookInventoryApplication

fun main(args: Array<String>) {
    runApplication<BookInventoryApplication>(*args)
}
