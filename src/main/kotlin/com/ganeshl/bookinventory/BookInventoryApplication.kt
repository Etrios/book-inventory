package com.ganeshl.bookinventory

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@OpenAPIDefinition(info = Info(title = "Book Inventory Service", version = "1.0", description = "API for managing book inventory"))
@SecurityScheme( // Add this annotation
    name = "basicAuth", // This is an arbitrary name you'll reference later
    type = SecuritySchemeType.HTTP,
    scheme = "basic",
    description = "Basic Authentication for API access"
)
class BookInventoryApplication

fun main(args: Array<String>) {
    runApplication<BookInventoryApplication>(*args)
}
