package com.example.sudokusolver.domain

class SudokuBoard(values: IntArray = IntArray(81)) {
    private val cells = values.copyOf()

    init {
        require(cells.size == 81) { "A Sudoku board must contain 81 cells" }
        require(cells.all { value -> value in 0..9 }) { "Cell values must be in 0..9" }
    }

    operator fun get(index: Int): Int {
        require(index in 0..80) { "Index must be in 0..80" }
        return cells[index]
    }

    operator fun get(position: CellPosition): Int = get(position.index)

    fun withValue(index: Int, value: Int): SudokuBoard {
        require(index in 0..80) { "Index must be in 0..80" }
        require(value in 0..9) { "Cell value must be in 0..9" }
        val updatedCells = cells.copyOf()
        updatedCells[index] = value
        return SudokuBoard(updatedCells)
    }

    fun withValue(position: CellPosition, value: Int): SudokuBoard =
        withValue(position.index, value)

    val clueCount: Int
        get() = cells.count { value -> value != 0 }

    // Complete means filled; SudokuValidator checks whether the digits obey Sudoku rules.
    val isComplete: Boolean
        get() = cells.all { value -> value != 0 }

    fun toIntArray(): IntArray = cells.copyOf()
}
