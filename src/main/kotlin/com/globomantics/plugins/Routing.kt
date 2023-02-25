package com.globomantics.plugins

import com.globomantics.datamodel.DatabaseContext
import com.globomantics.services.Database
import com.globomantics.datamodel.BookDbModel
import com.globomantics.datamodel.CategoryDbModel
import com.globomantics.datamodel.UserDbModel
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.example.ISBNValidator

@Serializable
data class Book(
    val title: String,
    val isbn: String,
    val authors: String,
    val category: Int)

@Serializable
data class User(val name: String)

@Serializable
data class Category(val name: String)

fun Application.configureRouting() {
    routing {
        route("/book") {
            get {
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                val books = DatabaseContext.getEntities<BookDbModel>(connection)
                call.respond(
                    books.map {
                        Book(
                            it.title,
                            it.isbn,
                            it.authors,
                            it.category ?: -1)
                    }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val result = mutableListOf<Book>()
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    connection.use {
                        it.createStatement().use { stmt ->
                            val sql = "SELECT TITLE, ISBN, AUTHORS, CATEGORY FROM BOOK WHERE ID=${id}"
                            val queryResult = stmt.executeQuery(sql)
                            while (queryResult.next()) {
                                val title = queryResult.getString("TITLE")
                                val isbn = queryResult.getString("ISBN")
                                val authors = queryResult.getString("AUTHORS")
                                val category = queryResult.getInt("CATEGORY")
                                result.add(Book(title, isbn, authors, category))
                            }
                        }
                    }
                    call.respond(result)
                }
            }
            put("/{id}") {
                val bookId = call.parameters["id"]?.toIntOrNull()
                val operation = call.parameters["operation"]
                val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                if (bookId == null || operation == null || userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid bookId or userId")
                } else {
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    connection.use {
                        it.createStatement().use { stmt ->
                            if (operation == "rent") {
                                val sql = "UPDATE BOOK SET RENTER_ID=${userId} WHERE ID=${bookId}"
                                stmt.execute(sql)
                            } else if (operation == "return") {
                                val sql = "UPDATE BOOK SET RENTER_ID=null WHERE ID=$bookId"
                                stmt.execute(sql)
                            } else {
                            }
                        }
                    }
                }
            }
            post {
                val book = call.receive<Book>()
                if (ISBNValidator.validate(book.isbn).result == "failed") {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ISBN")
                } else {
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    DatabaseContext.addEntity(
                        connection,
                        BookDbModel(
                            book.title,
                            book.isbn,
                            book.authors,
                            null,
                            book.category))
                }
            }
        }
        route("/user") {
            get {
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                val users = DatabaseContext.getEntities<UserDbModel>(connection)
                call.respond(users.map { User(it.name) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val result = mutableListOf<String>()
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    connection.use {
                        it.createStatement().use { stmt ->
                            val sql = "SELECT NAME FROM USER WHERE ID=${id}"
                            val queryResult = stmt.executeQuery(sql)
                            while (queryResult.next()) {
                                result.add(queryResult.getString("NAME"))
                            }
                        }
                    }
                    call.respond(result)
                }
            }
            post {
                val name = call.receiveText()
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                DatabaseContext.addEntity(connection, UserDbModel(name))
            }
        }
        route("/category") {
            get {
                val result = mutableListOf<String>()
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                val categories = DatabaseContext.getEntities<CategoryDbModel>(connection)
                call.respond(categories.map { Category(it.name) }.toTypedArray())
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val result = mutableListOf<String>()
                    val connection = Database.getConnection()
                    DatabaseContext.ensureCreated(connection)
                    connection.use {
                        it.createStatement().use { stmt ->
                            val sql = "SELECT NAME FROM CATEGORY WHERE ID=${id}"
                            val queryResult = stmt.executeQuery(sql)
                            while (queryResult.next()) {
                                result.add(queryResult.getString("NAME"))
                            }
                        }
                    }
                    call.respond(result)
                }
            }
            post {
                val name = call.receiveText()
                val connection = Database.getConnection()
                DatabaseContext.ensureCreated(connection)
                DatabaseContext.addEntity(connection, CategoryDbModel(name))
            }
        }
    }
}
