import java.util.*
import kotlin.collections.HashMap

open class MapBackedSpreadsheet<C : Any, R : Any, V : Any>(
    private val rowIdFactory: () -> R,
    private val columnIdFactory: () -> C
) : Spreadsheet<C, R, V> {
    private val rowOrder: MutableList<R> = mutableListOf()
    private val colOrder: MutableList<C> = mutableListOf()

    /** Row -> (Column -> Value) */
    private val contentData: MutableMap<R, MutableMap<C, V>> = HashMap()

    override operator fun get(col: C, row: R): V? = contentData[row]?.get(col)

    override fun rows() = rowOrder.toList()
    override fun columns() = colOrder.toList()

    fun insertRowAt(index: Int, row: R) {
        require(index >= 0 && index <= rowOrder.size) {"Index $index should be 0..${rowOrder.size}"}
        if (index == rowOrder.size) {
            rowOrder.add(row)
        } else {
            rowOrder.add(index, row)
        }
    }
    fun insertColumnAt(index: Int, column: C) {
        require(index >= 0 && index <= colOrder.size) {"Index $index should be 0..${colOrder.size}"}
        if (index == colOrder.size) {
            colOrder.add(column)
        } else {
            colOrder.add(index, column)
        }
    }

    override fun addRow(): R {
        val id = rowIdFactory()
        rowOrder += id
        return id
    }

    override fun addColumn(): C {
        val id = columnIdFactory()
        colOrder += id
        return id
    }

    override fun removeRow(row: R) {
        rowOrder.remove(row)
        clearRow(row)
    }

    override fun removeColumn(column: C) {
        colOrder.remove(column)
        clearColumn(column)
    }

    fun clearRow(row: R) {
        contentData.remove(row)
    }

    fun clearColumn(column: C) {
        for (rowMap in contentData.values) {
            rowMap.remove(column)
        }
    }

    override fun insertRow(row: R): R {
        val idx = rowOrder.indexOf(row)
        require(idx >= 0) {"Reference row $row is not in this Spreadsheet"}
        val id = rowIdFactory()
        rowOrder.add(idx, id)
        return id
    }

    override fun insertColumn(column: C): C {
        val idx = colOrder.indexOf(column)
        require(idx >= 0) {"Reference column $column is not in this Spreadsheet"}
        val id = columnIdFactory()
        colOrder.add(idx, id)
        return id
    }

    override fun editCell(column: C, row: R, value: V?) {
        if (value == null) {
            contentData[row]?.remove(column)
            return
        }
        contentData.getOrPut(row) { mutableMapOf() }[column] = value
    }

    override fun toString(): String = buildString {
        appendLine("Spreadsheet(${rowOrder.size}×${colOrder.size})")
        for (r in rowOrder) {
            for (c in colOrder) {
                append(contentData[r]?.get(c) ?: "·").append(' ')
            }
            appendLine()
        }
    }

    @JvmInline value class RowId(val id: UUID)
    @JvmInline value class ColumnId(val id: UUID)

    class UuidMapBackedSpreadsheet <V : Any> : MapBackedSpreadsheet<ColumnId, RowId, V>(
        rowIdFactory = { RowId(UUID.randomUUID()) },
        columnIdFactory = { ColumnId(UUID.randomUUID()) }
    )
}
