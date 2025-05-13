import MapBackedSpreadsheet.*
import SpreadsheetOperation.*
import java.time.Clock
import java.util.*

class ReplicaSpreadsheet<V : Any>(
    val clock: Clock,
    val replicaId: UUID = UUID.randomUUID()
): Spreadsheet<ColumnId, RowId, V> {
    private var localSeq: Long = 0L
    private val creationOfRows = mutableMapOf<RowId, OperationId>()
    private val creationOfColumns = mutableMapOf<ColumnId, OperationId>()
    private val localOperations = TreeMap<OperationId, SpreadsheetOperation>()
    private val localSpreadsheet = UuidMapBackedSpreadsheet<Value<V>>()
    private val tombstones = mutableSetOf<UUID>()

    private data class Value<V>(var value: V?, var operationId: OperationId) {
        fun setIfNewer(newValue: V?, newOperationId: OperationId): Boolean {
            if (newOperationId > operationId) {
                value = newValue
                return true
            }
            return false
        }
    }

    private fun nextOpId() = OperationId(replicaId, clock.instant(), ++localSeq)

    override fun addRow(): RowId {
        val nextTo = localSpreadsheet.rows().lastOrNull()
        val row = localSpreadsheet.addRow()
        val operationId = nextOpId()
        creationOfRows[row] = operationId
        localOperations[operationId] = InsertRowNextTo(row, nextTo)
        return row
    }

    override fun addColumn(): ColumnId {
        val nextTo = localSpreadsheet.columns().lastOrNull()
        val column = localSpreadsheet.addColumn()
        val operationId = nextOpId()
        creationOfColumns[column] = operationId
        localOperations[operationId] = InsertColumnNextTo(column, nextTo)
        return column
    }

    override fun rows(): List<RowId> {
        return localSpreadsheet.rows().filter { !tombstones.contains(it.id) }
    }

    override fun columns(): List<ColumnId> {
        return localSpreadsheet.columns().filter { !tombstones.contains(it.id) }
    }

    override fun get(col: ColumnId, row: RowId): V? {
        return localSpreadsheet[col, row]?.value
    }

    fun receiveActionSince(since: OperationId?): NavigableMap<OperationId, SpreadsheetOperation> {
        return if (since == null) localOperations else localOperations.tailMap(since, false)
    }

    fun apply(operations: SortedMap<OperationId, SpreadsheetOperation>) {
        operations.forEach { (id, operation) ->
            run {
                when (operation) {
                    is EditCell<*, *, *> -> applyEditCellLocally(id, operation as EditCell<ColumnId, RowId, V>)
                    is InsertRowNextTo<*> -> {
                        var index = localSpreadsheet.rows().indexOf(operation.nextRow)
                        while (localSpreadsheet.rows().lastIndex >= index + 1 && creationOfRows[localSpreadsheet.rows()[index + 1]]!! < id) index++;
                        localSpreadsheet.insertRowAt(index + 1, operation.newRow as RowId)
                        creationOfRows[operation.newRow] = id
                    }
                    is InsertColumnNextTo<*> -> {
                        var index = localSpreadsheet.columns().indexOf(operation.nextColumn)
                        while (localSpreadsheet.columns().lastIndex >= index + 1 && creationOfColumns[localSpreadsheet.columns()[index + 1]]!! < id) index++
                        localSpreadsheet.insertColumnAt(index + 1, operation.newColumn as ColumnId)
                        creationOfColumns[operation.newColumn] = id
                    }
                    is InsertRowPreviousTo<*> -> {
                        var idx = localSpreadsheet.rows().indexOf(operation.previousTo)
                        while (idx - 1 >= 0 && creationOfRows[localSpreadsheet.rows()[idx - 1]]!! > id) idx--
                        localSpreadsheet.insertRowAt(idx, operation.row as RowId)
                        creationOfRows[operation.row] = id
                    }
                    is InsertColumnPreviousTo<*> -> {
                        var idx = localSpreadsheet.columns().indexOf(operation.previousTo)
                        while (idx - 1 >= 0 && creationOfColumns[localSpreadsheet.columns()[idx - 1]]!! > id) idx--
                        localSpreadsheet.insertColumnAt(idx, operation.column as ColumnId)
                        creationOfColumns[operation.column] = id
                    }
                    is RemoveRow<*> -> {
                        tombstones.add((operation.row as RowId).id)
                        localSpreadsheet.clearRow(operation.row)
                    }
                    is RemoveColumn<*> -> {
                        tombstones.add((operation.column as ColumnId).id)
                        localSpreadsheet.clearColumn(operation.column)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun applyEditCellLocally(operationId: OperationId, cell: EditCell<ColumnId, RowId, V>): Boolean =
        localSpreadsheet[cell.column, cell.row]
            ?.setIfNewer(cell.value, operationId)
            ?: run {
                localSpreadsheet.editCell(cell.column, cell.row, Value(cell.value, operationId))
                true
            }

    override fun editCell(column: ColumnId, row: RowId, value: V?) {
        val opId = nextOpId()
        val operation = EditCell(column, row, value)
        val updated = applyEditCellLocally(opId, operation)
        if (updated) {
            localOperations[opId] = operation
        }
    }

    override fun insertColumn(column: ColumnId): ColumnId {
        val opId = nextOpId()
        val newColumn = localSpreadsheet.insertColumn(column)
        localOperations[opId] = InsertColumnPreviousTo(newColumn, column)
        creationOfColumns[newColumn] = opId
        return newColumn
    }

    override fun insertRow(row: RowId): RowId {
        val opId = nextOpId()
        val newRow = localSpreadsheet.insertRow(row)
        localOperations[opId] = InsertRowPreviousTo(newRow, row)
        creationOfRows[newRow] = opId
        return newRow
    }

    fun verifyEquals(other: ReplicaSpreadsheet<V>) {
        if (other.rows() != rows()) {
            throw IllegalStateException("Rows out of sync")
        }
        if (other.columns() != columns()) {
            throw IllegalStateException("Columns out of sync")
        }
    }

    override fun removeColumn(column: ColumnId) {
        tombstones.add(column.id)
        localSpreadsheet.clearColumn(column)
        localOperations[nextOpId()] = RemoveColumn(column)
    }

    override fun removeRow(row: RowId) {
        tombstones.add(row.id)
        localSpreadsheet.clearRow(row)
        localOperations[nextOpId()] = RemoveRow(row)
    }

    override fun toString(): String = buildString {
        appendLine("Spreadsheet(${rows().size}×${columns().size})")
        for (r in rows()) {
            for (c in columns()) {
                append(this@ReplicaSpreadsheet[c, r] ?: "·").append('\t')
            }
            appendLine()
        }
    }
}