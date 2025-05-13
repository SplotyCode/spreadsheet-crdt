interface Spreadsheet<C, R, V> {
    fun addRow(): R

    fun addColumn(): C

    fun removeRow(row: R)

    fun removeColumn(column: C)

    /** Inserts a row *before* the selected one [row] */
    fun insertRow(row: R): R

    /** Inserts a column *before* the selected one [column] */
    fun insertColumn(column: C): C

    fun editCell(column: C, row: R, value: V?)

    operator fun get(col: C, row: R): V?

    fun rows(): List<R>
    fun columns(): List<C>
}