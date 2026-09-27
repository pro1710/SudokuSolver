package com.example.sudokusolver.domain

data class CellPosition(
    val row: Int,
    val column: Int
) {
    init {
        require(row in 0..8) { "Row must be in 0..8" }
        require(column in 0..8) { "Column must be in 0..8" }
    }

    val index: Int
        get() = row * 9 + column

    companion object {
        fun fromIndex(index: Int): CellPosition {
            require(index in 0..80) { "Index must be in 0..80" }
            return CellPosition(row = index / 9, column = index % 9)
        }
    }
}
