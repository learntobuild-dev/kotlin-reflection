package com.example.services

import java.sql.Connection
import java.sql.DriverManager

class Database {
    companion object {

        fun getConnection(): Connection {
            Class.forName("org.sqlite.JDBC")
            return DriverManager.getConnection("jdbc:sqlite:test1.db")
        }
    }
}