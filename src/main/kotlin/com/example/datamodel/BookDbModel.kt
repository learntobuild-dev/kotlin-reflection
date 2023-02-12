package com.example.datamodel

@TableName("BOOK")
class BookDbModel {
    constructor(
        id: Int,
        title: String,
        isbn: String,
        authors: String,
        renterId: Int?,
        category: Int?) {
        this.id = id
        this.title = title
        this.isbn = isbn
        this.authors = authors
        this.renterId = renterId
        this.category = category
    }

    @PrimaryKey
    @ColumnName("ID")
    var id: Int = 0

    @ColumnName("TITLE")
    var title: String = ""

    @ColumnName("ISBN")
    var isbn: String = ""

    @ColumnName("AUTHORS")
    var authors: String = ""

    @ColumnName("RENTER_ID")
    var renterId: Int? = null

    @ColumnName("CATEGORY")
    var category: Int? = null
}