package com.ganeshl.bookinventory.repository

import com.ganeshl.bookinventory.model.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface BookRepository : JpaRepository<Book, Long> {
    fun findByIsbn(isbn: String): Optional<Book>

    @Query("SELECT b FROM Book b WHERE " +
            "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
            "(:genre IS NULL OR LOWER(b.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) AND " +
            "(:isbn IS NULL OR b.isbn = :isbn)")
    fun searchBooks(
        @Param("title") title: String?,
        @Param("author") author: String?,
        @Param("genre") genre: String?,
        @Param("isbn") isbn: String?
    ): List<Book>
}