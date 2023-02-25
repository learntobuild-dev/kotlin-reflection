package com.globomantics.datamodel

@TableName("BOOK")
class BookDbModel(): DbEntity() {
    constructor(
        title: String,
        isbn: String,
        authors: String,
        renterId: Int?,
        category: Int?) : this() {
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

    fun withRenterId(value: Int?): BookDbModel {
        return BookDbModel(title, isbn, authors, value, category)
    }
}