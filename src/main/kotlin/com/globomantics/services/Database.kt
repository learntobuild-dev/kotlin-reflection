package com.globomantics.services

import java.sql.Connection
import java.sql.DriverManager

class Database {
    companion object {
        fun getUserCount(): Int? {
            initialize()
            getConnection().use {
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
            initialize()
            getConnection().use {
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
            initialize()
            getConnection().use {
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
        fun initialize() {
            val connection = getConnection()
            connection.use { connection ->
                val tables = getTables(connection)
                if (!tables.contains("CATEGORY")) {
                    createCategoryTable(connection)
                }
                if (!tables.contains("USER")) {
                    createUserTable(connection)
                }
                if (!tables.contains("BOOK")) {
                    createBookTable(connection)
                }
            }
        }
        private fun getTables(conn: Connection): Array<String> {
            val stmt = conn.createStatement()
            val sql = "select name from sqlite_schema where type = 'table'"
            val queryResult = stmt.executeQuery(sql)
            val result: MutableList<String> = mutableListOf()
            while (queryResult.next()) {
                result.add(queryResult.getString("name"))
            }
            stmt.close()
            return result.toTypedArray()
        }
        private fun createCategoryTable(conn: Connection): Unit {
            val stmt = conn.createStatement()
            val sql = "CREATE TABLE CATEGORY " +
                    "(ID INT PRIMARY KEY     NOT NULL," +
                    " NAME           TEXT    NOT NULL)"
            stmt.executeUpdate(sql)
            stmt.close()
        }
        private fun createUserTable(conn: Connection): Unit {
            val stmt = conn.createStatement()
            val sql = "CREATE TABLE USER " +
                    "(ID INT PRIMARY KEY     NOT NULL," +
                    " NAME           TEXT    NOT NULL)"
            stmt.executeUpdate(sql)
            stmt.close()
        }
        private fun createBookTable(conn: Connection): Unit {
            val stmt = conn.createStatement()
            val sql = "CREATE TABLE BOOK " +
                    "(ID INT PRIMARY KEY    NOT NULL," +
                    " TITLE          TEXT   NOT NULL," +
                    " ISBN           TEXT   NOT NULL," +
                    " AUTHORS        TEXT   NOT NULL," +
                    " RENTER_ID      TEXT   NULL," +
                    " CATEGORY       INT   NULL)"
            stmt.executeUpdate(sql)
            stmt.close()
        }
        fun getConnection(): Connection {
            Class.forName("org.sqlite.JDBC")
            return DriverManager.getConnection("jdbc:sqlite:BookStore.db")
        }
    }
}