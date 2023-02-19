package com.example.services

import com.example.datamodel.BookDbModel
import com.example.datamodel.CategoryDbModel
import com.example.datamodel.DatabaseContext
import com.example.datamodel.UserDbModel
import com.example.interfaces.IRepository
import com.example.plugins.Book
import com.example.plugins.Category
import com.example.plugins.User

class Repository(val context: DatabaseContext): IRepository {
    override fun getAllBooks(): Array<Book> {
        val books = context.getEntities<BookDbModel>(null)
        return books.map { Mapper.map<BookDbModel, Book>(it) }.toTypedArray()
    }

    override fun getBook(id: Int): Book {
        val books = context.getEntities<BookDbModel>(Pair(BookDbModel::id, id))
        return books.map {Mapper.map<BookDbModel, Book>(it) }.first()
    }

    override fun setRenterId(bookId: Int, renterId: Int?) {
        context.updateEntities<BookDbModel>(Pair(BookDbModel::id, bookId)) {
            it.withRenterId(renterId)
        }
    }

    override fun addBook(book: Book) {
        context.addEntity(Mapper.map<Book, BookDbModel>(book))
    }

    override fun getAllUsers(): Array<User> {
        val users = context.getEntities<UserDbModel>(null)
        return users.map { Mapper.map<UserDbModel, User>(it) }.toTypedArray()
    }

    override fun getUser(id: Int): User {
        val users = context.getEntities<UserDbModel>(Pair(UserDbModel::id, id))
        return users.map { Mapper.map<UserDbModel, User>(it) }.first()
    }

    override fun addUser(user: User) {
        context.addEntity(Mapper.map<User, UserDbModel>(user))
    }

    override fun getAllCategories(): Array<Category> {
        val categories = context.getEntities<CategoryDbModel>(null)
        return categories.map { Mapper.map<CategoryDbModel, Category>(it) }.toTypedArray()
    }

    override fun getCategory(id: Int): Category {
        val categories = context.getEntities<CategoryDbModel>(Pair(CategoryDbModel::id, id))
        return categories.map { Mapper.map<CategoryDbModel, Category>(it) }.first()
    }

    override fun addCategory(category: Category) {
        context.addEntity(Mapper.map<Category, CategoryDbModel>(category))
    }
}