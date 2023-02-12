package com.example.services

import com.example.datamodel.DatabaseContext
import java.sql.Connection
import java.sql.DriverManager

class Database {
    companion object {
        fun getUserCount(): Int? {
            val connection = getConnection()
            DatabaseContext.ensureCreated(connection)
            connection.use {
                it.createStatement().use { stmt ->
                    val sql = "SELECT COUNT(*) FROM USER"
                    val queryResult = stmt.executeQuery(sql)
                    while (queryResult.next()) {
                        return queryResult.getInt(1)
                    }
                }
            }
            return null
        }

        fun getCategoryCount(): Int? {
            val connection = getConnection()
            DatabaseContext.ensureCreated(connection)
            connection.use {
                it.createStatement().use { stmt ->
                    val sql = "SELECT COUNT(*) FROM CATEGORY"
                    val queryResult = stmt.executeQuery(sql)
                    while (queryResult.next()) {
                        return queryResult.getInt(1)
                    }
                }
            }
            return null
        }

        fun getBookCount(): Int? {
            val connection = getConnection()
            DatabaseContext.ensureCreated(connection)
            connection.use {
                it.createStatement().use { stmt ->
                    val sql = "SELECT COUNT(*) FROM BOOK"
                    val queryResult = stmt.executeQuery(sql)
                    while (queryResult.next()) {
                        return queryResult.getInt(1)
                    }
                }
            }
            return null
        }

        fun getConnection(): Connection {
            Class.forName("org.sqlite.JDBC")
            return DriverManager.getConnection("jdbc:sqlite:test1.db")
        }
    }
}