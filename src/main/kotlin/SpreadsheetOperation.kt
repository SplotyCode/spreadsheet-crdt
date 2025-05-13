sealed interface SpreadsheetOperation {
    data class InsertRowPreviousTo<R>(val row: R, val previousTo: R) : SpreadsheetOperation
    data class InsertColumnPreviousTo<C>(val column: C, val previousTo: C) : SpreadsheetOperation
    data class RemoveRow<R>(val row: R) : SpreadsheetOperation
    data class RemoveColumn<C>(val column: C) : SpreadsheetOperation
    data class InsertRowNextTo<R>(val newRow: R, val nextRow: R?)  : SpreadsheetOperation
    data class InsertColumnNextTo<C>(val newColumn: C, val nextColumn: C?) : SpreadsheetOperation
    data class EditCell<C, R, V>(val column: C, val row: R, val value: V?) : SpreadsheetOperation
}