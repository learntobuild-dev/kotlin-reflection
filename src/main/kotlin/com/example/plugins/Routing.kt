package com.example.plugins

import com.example.datamodel.DatabaseContext
import com.example.services.Database
import com.example.services.Database.Companion.ensureCreated
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class Book(val title: String, val isbn: String, val authors: Array<String>, val category: Int)

fun Application.configureRouting() {
    routing {
        route("/book") {
            get {
                val result = mutableListOf<Book>()
                ensureCreated(typeOf<DatabaseContext>())
                Database.getConnection().use {
                    it.createStatement().use { stmt ->
                        val sql = "SELECT TITLE, ISBN, AUTHORS, CATEGORY FROM BOOK"
                        val queryResult = stmt.executeQuery(sql)
                        while (queryResult.next()) {
                            val title = queryResult.getString("TITLE")
                            val isbn = queryResult.getString("ISBN")
                            val authors = queryResult.getString("AUTHORS")
                            val category = queryResult.getInt("CATEGORY")
                            result.add(Book(title, isbn, authors.split(",").toTypedArray(), category))
                        }
                    }
                }
                call.respond(result)
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val result = mutableListOf<Book>()
                    ensureCreated(typeOf<DatabaseContext>())
                    Database.getConnection().use {
                        it.createStatement().use { stmt ->
                            val sql = "SELECT TITLE, ISBN, AUTHORS, CATEGORY FROM BOOK WHERE ID=${id}"
                            val queryResult = stmt.executeQuery(sql)
                            while (queryResult.next()) {
                                val title = queryResult.getString("TITLE")
                                val isbn = queryResult.getString("ISBN")
                                val authors = queryResult.getString("AUTHORS")
                                val category = queryResult.getInt("CATEGORY")
                                result.add(Book(title, isbn, authors.split(",").toTypedArray(), category))
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
                    ensureCreated(typeOf<DatabaseContext>())
                    Database.getConnection().use {
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
                ensureCreated(typeOf<DatabaseContext>())
                Database.getConnection().use {
                    it.createStatement().use { stmt ->
                        val id = (Database.getBookCount() ?: 0) + 1
                        val sql =
                            "INSERT INTO BOOK VALUES " +
                                    "(${id}, '${book.title}', " +
                                    "'${book.isbn}', " +
                                    "'${book.authors.joinToString(",")}', " +
                                    "${book.category})"
                        stmt.execute(sql)
                    }
                }
            }
        }
        route("/user") {
            get {
                val result = mutableListOf<String>()
                ensureCreated(typeOf<DatabaseContext>())
                Database.getConnection().use {
                    it.createStatement().use { stmt ->
                        val sql = "SELECT NAME FROM USER"
                        val queryResult = stmt.executeQuery(sql)
                        while (queryResult.next()) {
                            result.add(queryResult.getString("NAME"))
                        }
                    }
                }
                call.respond(result)
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val result = mutableListOf<String>()
                    ensureCreated(typeOf<DatabaseContext>())
                    Database.getConnection().use {
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
                ensureCreated(typeOf<DatabaseContext>())
                Database.getConnection().use {
                    it.createStatement().use { stmt ->
                        val id = (Database.getUserCount() ?: 0) + 1
                        val sql = "INSERT INTO USER VALUES (${id}, '${name}')"
                        stmt.execute(sql)
                    }
                }
            }
        }
        route("/category") {
            get {
                val result = mutableListOf<String>()
                ensureCreated(typeOf<DatabaseContext>())
                Database.getConnection().use {
                    it.createStatement().use { stmt ->
                        val sql = "SELECT NAME FROM CATEGORY"
                        val queryResult = stmt.executeQuery(sql)
                        while (queryResult.next()) {
                            result.add(queryResult.getString("NAME"))
                        }
                    }
                }
                call.respond(result)
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                } else {
                    val result = mutableListOf<String>()
                    ensureCreated(typeOf<DatabaseContext>())
                    Database.getConnection().use {
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
                ensureCreated(typeOf<DatabaseContext>())
                Database.getConnection().use {
                    it.createStatement().use { stmt ->
                        val id = (Database.getCategoryCount() ?: 0) + 1
                        val sql = "INSERT INTO CATEGORY VALUES (${id}, '${name}')"
                        stmt.execute(sql)
                    }
                }
            }
        }
    }
}
