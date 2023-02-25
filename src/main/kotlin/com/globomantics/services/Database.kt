package com.globomantics.services

import com.globomantics.datamodel.ColumnName
import com.globomantics.datamodel.DatabaseContext
import com.globomantics.datamodel.PrimaryKey
import com.globomantics.datamodel.TableName
import java.sql.Connection
import java.sql.DriverManager

class Database {
    companion object {

        fun getConnection(): Connection {
            Class.forName("org.sqlite.JDBC")
            return DriverManager.getConnection("jdbc:sqlite:BookStore.db")
        }
    }
}