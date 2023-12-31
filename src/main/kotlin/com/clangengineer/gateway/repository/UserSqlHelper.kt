package com.clangengineer.gateway.repository

import org.springframework.data.relational.core.sql.Column
import org.springframework.data.relational.core.sql.Expression
import org.springframework.data.relational.core.sql.Table

class UserSqlHelper {
    fun getColumns(table: Table, columnPrefix: String): MutableList<Expression> {
        val columns = mutableListOf<Expression>()
        columns.add(Column.aliased("id", table, columnPrefix + "_id"))
        columns.add(Column.aliased("login", table, columnPrefix + "_login"))
        columns.add(Column.aliased("password_hash", table, columnPrefix + "_password"))
        columns.add(Column.aliased("first_name", table, columnPrefix + "_first_name"))
        columns.add(Column.aliased("last_name", table, columnPrefix + "_last_name"))
        columns.add(Column.aliased("email", table, columnPrefix + "_email"))
        columns.add(Column.aliased("activated", table, columnPrefix + "_activated"))
        columns.add(Column.aliased("lang_key", table, columnPrefix + "_lang_key"))
        columns.add(Column.aliased("image_url", table, columnPrefix + "_image_url"))
        columns.add(Column.aliased("activation_key", table, columnPrefix + "_activation_key"))
        columns.add(Column.aliased("reset_key", table, columnPrefix + "_reset_key"))
        columns.add(Column.aliased("reset_date", table, columnPrefix + "_reset_date"))
        return columns
    }
}
