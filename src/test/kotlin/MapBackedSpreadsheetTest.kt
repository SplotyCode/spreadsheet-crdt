import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class MapBackedSpreadsheetTest {
    private lateinit var spreadsheet: MapBackedSpreadsheet<MapBackedSpreadsheet.ColumnId, MapBackedSpreadsheet.RowId, String>
    private var idCounter = 0L

    private fun predictableRowId(id: Long) = MapBackedSpreadsheet.RowId(UUID(0, id))
    private fun predictableColId(id: Long) = MapBackedSpreadsheet.ColumnId(UUID(0, id))

    @BeforeEach
    fun setUp() {
        idCounter = 0
        spreadsheet = MapBackedSpreadsheet(
            rowIdFactory = { MapBackedSpreadsheet.RowId(UUID(0, idCounter++)) },
            columnIdFactory = { MapBackedSpreadsheet.ColumnId(UUID(0, idCounter++)) }
        )
    }

    @Test
    fun `initial state is empty`() {
        assertThat(spreadsheet.rows()).isEmpty()
        assertThat(spreadsheet.columns()).isEmpty()
        assertThat(spreadsheet[predictableColId(-1), predictableRowId(-1)]).isNull()
    }

    @Test
    fun `addRow adds a new row and returns its ID`() {
        val r1 = spreadsheet.addRow()
        assertThat(r1).isEqualTo(predictableRowId(0))
        assertThat(spreadsheet.rows()).containsExactly(r1)

        val r2 = spreadsheet.addRow()
        assertThat(r2).isEqualTo(predictableRowId(1))
        assertThat(spreadsheet.rows()).containsExactly(r1, r2)
    }

    @Test
    fun `addColumn adds a new column and returns its ID`() {
        val c1 = spreadsheet.addColumn()
        assertThat(c1).isEqualTo(predictableColId(0))
        assertThat(spreadsheet.columns()).containsExactly(c1)

        val c2 = spreadsheet.addColumn()
        assertThat(c2).isEqualTo(predictableColId(1))
        assertThat(spreadsheet.columns()).containsExactly(c1, c2)
    }

    @Test
    fun `addRow and addColumn generate sequential unique IDs`() {
        val r1 = spreadsheet.addRow()
        val c1 = spreadsheet.addColumn()
        val r2 = spreadsheet.addRow()
        val c2 = spreadsheet.addColumn()

        assertThat(r1).isEqualTo(predictableRowId(0))
        assertThat(c1).isEqualTo(predictableColId(1))
        assertThat(r2).isEqualTo(predictableRowId(2))
        assertThat(c2).isEqualTo(predictableColId(3))

        val ids = setOf(r1.id, c1.id, r2.id, c2.id)
        assertThat(ids).hasSize(4)

        assertThat(spreadsheet.rows()).containsExactly(r1, r2)
        assertThat(spreadsheet.columns()).containsExactly(c1, c2)
    }

    @Test
    fun `editCell adds or updates cell value`() {
        val r1 = spreadsheet.addRow()
        val c1 = spreadsheet.addColumn()
        val r2 = spreadsheet.addRow()
        val c2 = spreadsheet.addColumn()

        assertThat(spreadsheet[c1, r1]).isNull()
        assertThat(spreadsheet[c2, r2]).isNull()

        spreadsheet.editCell(c1, r1, "A1")
        spreadsheet.editCell(c2, r2, "B2")

        assertThat(spreadsheet[c1, r1]).isEqualTo("A1")
        assertThat(spreadsheet[c2, r2]).isEqualTo("B2")
        assertThat(spreadsheet[c1, r2]).isNull()
        assertThat(spreadsheet[c2, r1]).isNull()

        spreadsheet.editCell(c1, r1, "A1_updated")
        assertThat(spreadsheet[c1, r1]).isEqualTo("A1_updated")

        val nonExistentRow = predictableRowId(99)
        val nonExistentCol = predictableColId(99)
        spreadsheet.editCell(nonExistentCol, nonExistentRow, "Z99")
        assertThat(spreadsheet[nonExistentCol, nonExistentRow]).isEqualTo("Z99")
        assertThat(spreadsheet.rows()).containsExactly(r1, r2)
        assertThat(spreadsheet.columns()).containsExactly(c1, c2)
    }

    @Test
    fun `editCell with null value removes cell content`() {
        val r1 = spreadsheet.addRow()
        val c1 = spreadsheet.addColumn()

        spreadsheet.editCell(c1, r1, "A1")
        assertThat(spreadsheet[c1, r1]).isEqualTo("A1")

        spreadsheet.editCell(c1, r1, null)
        assertThat(spreadsheet[c1, r1]).isNull()

        val nonExistentRow = predictableRowId(99)
        val nonExistentCol = predictableColId(99)
        spreadsheet.editCell(nonExistentCol, nonExistentRow, null)
        assertThat(spreadsheet[nonExistentCol, nonExistentRow]).isNull()
    }

    @Test
    fun `removeRow removes row from order and clears its data`() {
        val r1 = spreadsheet.addRow()
        val c1 = spreadsheet.addColumn()
        val r2 = spreadsheet.addRow()
        val c2 = spreadsheet.addColumn()
        spreadsheet.editCell(c1, r1, "A1")
        spreadsheet.editCell(c2, r1, "B1")
        spreadsheet.editCell(c1, r2, "A2")
        spreadsheet.editCell(c2, r2, "B2")

        spreadsheet.removeRow(r1)

        assertThat(spreadsheet.rows()).containsExactly(r2)
        assertThat(spreadsheet.columns()).containsExactly(c1, c2)
        assertThat(spreadsheet[c1, r1]).isNull()
        assertThat(spreadsheet[c2, r1]).isNull()
        assertThat(spreadsheet[c1, r2]).isEqualTo("A2")
        assertThat(spreadsheet[c2, r2]).isEqualTo("B2")

        spreadsheet.removeRow(r2)
        assertThat(spreadsheet.rows()).isEmpty()
        assertThat(spreadsheet[c1, r2]).isNull()
        assertThat(spreadsheet[c2, r2]).isNull()
    }

    @Test
    fun `removeColumn removes column from order and clears its data`() {
        val r1 = spreadsheet.addRow()
        val c1 = spreadsheet.addColumn()
        val r2 = spreadsheet.addRow()
        val c2 = spreadsheet.addColumn()
        spreadsheet.editCell(c1, r1, "A1")
        spreadsheet.editCell(c2, r1, "B1")
        spreadsheet.editCell(c1, r2, "A2")
        spreadsheet.editCell(c2, r2, "B2")

        spreadsheet.removeColumn(c1)

        assertThat(spreadsheet.rows()).containsExactly(r1, r2)
        assertThat(spreadsheet.columns()).containsExactly(c2)
        assertThat(spreadsheet[c1, r1]).isNull()
        assertThat(spreadsheet[c2, r1]).isEqualTo("B1")
        assertThat(spreadsheet[c1, r2]).isNull()
        assertThat(spreadsheet[c2, r2]).isEqualTo("B2")

        spreadsheet.removeColumn(c2)
        assertThat(spreadsheet.columns()).isEmpty()
        assertThat(spreadsheet[c2, r1]).isNull()
        assertThat(spreadsheet[c2, r2]).isNull()
    }

    @Test
    fun `clearRow removes data but not row order`() {
        val r1 = spreadsheet.addRow()
        val c1 = spreadsheet.addColumn()
        spreadsheet.editCell(c1, r1, "A1")

        spreadsheet.clearRow(r1)

        assertThat(spreadsheet.rows()).containsExactly(r1)
        assertThat(spreadsheet[c1, r1]).isNull()
    }

    @Test
    fun `clearColumn removes data but not column order`() {
        val r1 = spreadsheet.addRow()
        val c1 = spreadsheet.addColumn()
        spreadsheet.editCell(c1, r1, "A1")

        spreadsheet.clearColumn(c1)

        assertThat(spreadsheet.columns()).containsExactly(c1)
        assertThat(spreadsheet[c1, r1]).isNull()
    }

    @Test
    fun `remove non-existent row or column does nothing`() {
        val r1 = spreadsheet.addRow()
        val c1 = spreadsheet.addColumn()
        spreadsheet.editCell(c1, r1, "A1")

        val nonExistentRow = predictableRowId(99)
        val nonExistentCol = predictableColId(99)

        spreadsheet.removeRow(nonExistentRow)
        assertThat(spreadsheet.rows()).containsExactly(r1)
        assertThat(spreadsheet[c1, r1]).isEqualTo("A1")

        spreadsheet.removeColumn(nonExistentCol)
        assertThat(spreadsheet.columns()).containsExactly(c1)
        assertThat(spreadsheet[c1, r1]).isEqualTo("A1")
    }

    @Test
    fun `insertRow inserts a new row before the specified row`() {
        val r1 = spreadsheet.addRow()
        val r2 = spreadsheet.addRow()

        val rNew = spreadsheet.insertRow(r2)
        assertThat(rNew).isEqualTo(predictableRowId(2))
        assertThat(spreadsheet.rows()).containsExactly(r1, rNew, r2)
    }

    @Test
    fun `insertRow throws if reference row does not exist`() {
        val r1 = spreadsheet.addRow()
        val nonExistentRow = predictableRowId(99)

        val exception = assertThrows<IllegalArgumentException> {
            spreadsheet.insertRow(nonExistentRow)
        }
        assertThat(exception.message).isEqualTo("Reference row ${nonExistentRow} is not in this Spreadsheet")
        assertThat(spreadsheet.rows()).containsExactly(r1)
    }

    @Test
    fun `insertColumn inserts a new column before the specified column`() {
        val c1 = spreadsheet.addColumn()
        val c2 = spreadsheet.addColumn()

        val cNew = spreadsheet.insertColumn(c2)
        assertThat(cNew).isEqualTo(predictableColId(2))
        assertThat(spreadsheet.columns()).containsExactly(c1, cNew, c2)
    }

    @Test
    fun `insertColumn throws if reference column does not exist`() {
        val c1 = spreadsheet.addColumn()
        val nonExistentCol = predictableColId(99)

        val exception = assertThrows<IllegalArgumentException> {
            spreadsheet.insertColumn(nonExistentCol)
        }
        assertThat(exception.message).isEqualTo("Reference column $nonExistentCol is not in this Spreadsheet")
        assertThat(spreadsheet.columns()).containsExactly(c1)
    }

    @Test
    fun `insertRowAt inserts row at specified index`() {
        val r1 = predictableRowId(10)
        val r2 = predictableRowId(11)
        val r3 = predictableRowId(12)
        val r4 = predictableRowId(13)

        spreadsheet.insertRowAt(0, r1)
        assertThat(spreadsheet.rows()).containsExactly(r1)

        spreadsheet.insertRowAt(1, r3)
        assertThat(spreadsheet.rows()).containsExactly(r1, r3)

        spreadsheet.insertRowAt(1, r2)
        assertThat(spreadsheet.rows()).containsExactly(r1, r2, r3)

        spreadsheet.insertRowAt(spreadsheet.rows().size, r4)
        assertThat(spreadsheet.rows()).containsExactly(r1, r2, r3, r4)
    }

    @Test
    fun `insertRowAt throws for negative index`() {
        val r1 = predictableRowId(10)
        assertThrows<IllegalArgumentException> {
            spreadsheet.insertRowAt(-1, r1)
        }
    }

    @Test
    fun `insertRowAt throws for index greater than size`() {
        val r1 = predictableRowId(10)
        spreadsheet.addRow()
        assertThrows<IllegalArgumentException> {
            spreadsheet.insertRowAt(2, r1)
        }
    }

    @Test
    fun `insertColumnAt inserts column at specified index`() {
        val c1 = predictableColId(10)
        val c2 = predictableColId(11)
        val c3 = predictableColId(12)
        val c4 = predictableColId(13)

        spreadsheet.insertColumnAt(0, c1)
        assertThat(spreadsheet.columns()).containsExactly(c1)

        spreadsheet.insertColumnAt(1, c3)
        assertThat(spreadsheet.columns()).containsExactly(c1, c3)

        spreadsheet.insertColumnAt(1, c2)
        assertThat(spreadsheet.columns()).containsExactly(c1, c2, c3)

        spreadsheet.insertColumnAt(spreadsheet.columns().size, c4)
        assertThat(spreadsheet.columns()).containsExactly(c1, c2, c3, c4)
    }

    @Test
    fun `insertColumnAt throws for negative index`() {
        val c1 = predictableColId(10)
        assertThrows<IllegalArgumentException> {
            spreadsheet.insertColumnAt(-1, c1)
        }
    }

    @Test
    fun `insertColumnAt throws for index greater than size`() {
        val c1 = predictableColId(10)
        spreadsheet.addColumn()
        assertThrows<IllegalArgumentException> {
            spreadsheet.insertColumnAt(2, c1)
        }
    }

    @Test
    fun `toString returns expected representation`() {
        assertThat(spreadsheet.toString()).isEqualTo("Spreadsheet(0×0)\n")

        val r1 = spreadsheet.addRow()
        val c1 = spreadsheet.addColumn()
        val r2 = spreadsheet.addRow()
        val c2 = spreadsheet.addColumn()

        val expectedEmptyGrid = """
            Spreadsheet(2×2)
            · · 
            · · 
            """.trimIndent() + "\n"
        assertThat(spreadsheet.toString()).isEqualTo(expectedEmptyGrid)

        spreadsheet.editCell(c1, r1, "A1")
        spreadsheet.editCell(c2, r2, "B2")

        val expectedWithData = """
            Spreadsheet(2×2)
            A1 · 
            · B2 
            """.trimIndent() + "\n"
        assertThat(spreadsheet.toString()).isEqualTo(expectedWithData)
    }

    @Test
    fun `row and column order remains stable after removals`() {
        val rows = List(4) { spreadsheet.addRow() }
        val cols = List(4) { spreadsheet.addColumn() }

        assertThat(spreadsheet.rows()).containsExactlyElementsOf(rows)
        assertThat(spreadsheet.columns()).containsExactlyElementsOf(cols)

        val rowToRemove = rows[1]
        val colToRemove = cols[1]
        spreadsheet.removeRow(rowToRemove)
        spreadsheet.removeColumn(colToRemove)

        val expectedRows = listOf(rows[0], rows[2], rows[3])
        val expectedCols = listOf(cols[0], cols[2], cols[3])

        assertThat(spreadsheet.rows()).containsExactlyElementsOf(expectedRows)
        assertThat(spreadsheet.columns()).containsExactlyElementsOf(expectedCols)
    }
}