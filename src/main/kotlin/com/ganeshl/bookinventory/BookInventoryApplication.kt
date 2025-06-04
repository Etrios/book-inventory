package com.ganeshl.bookinventory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BookInventoryApplication

fun main(args: Array<String>) {
    runApplication<BookInventoryApplication>(*args)
}
