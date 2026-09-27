package com.example.sudokusolver.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CellPositionTest {
    @Test
    fun `row and column convert to the expected index`() {
        assertEquals(0, CellPosition(0, 0).index)
        assertEquals(23, CellPosition(2, 5).index)
        assertEquals(80, CellPosition(8, 8).index)
    }

    @Test
    fun `index converts to the expected row and column`() {
        assertEquals(CellPosition(0, 0), CellPosition.fromIndex(0))
        assertEquals(CellPosition(2, 5), CellPosition.fromIndex(23))
        assertEquals(CellPosition(8, 8), CellPosition.fromIndex(80))
    }

    @Test
    fun `every valid position survives conversion to an index and back`() {
        for (row in 0..8) {
            for (column in 0..8) {
                val position = CellPosition(row, column)
                assertEquals(position, CellPosition.fromIndex(position.index))
            }
        }
    }

    @Test
    fun `invalid rows are rejected`() {
        for (row in listOf(-1, 9)) {
            assertThrows(IllegalArgumentException::class.java) { CellPosition(row, 0) }
        }
    }

    @Test
    fun `invalid columns are rejected`() {
        for (column in listOf(-1, 9)) {
            assertThrows(IllegalArgumentException::class.java) { CellPosition(0, column) }
        }
    }

    @Test
    fun `invalid indices are rejected`() {
        for (index in listOf(-1, 81)) {
            assertThrows(IllegalArgumentException::class.java) { CellPosition.fromIndex(index) }
        }
    }
}
