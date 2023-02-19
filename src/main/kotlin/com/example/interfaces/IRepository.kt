package com.example.interfaces

import com.example.plugins.Book
import com.example.plugins.Category
import com.example.plugins.User

interface IRepository {
    fun getAllBooks(): Array<Book>
    fun getBook(id: Int): Book
    fun setRenterId(bookId: Int, renterId: Int?)
    fun addBook(book: Book)
    fun getAllUsers(): Array<User>
    fun getUser(id: Int): User
    fun addUser(user: User)
    fun getAllCategories(): Array<Category>
    fun getCategory(id: Int): Category
    fun addCategory(category: Category)
}