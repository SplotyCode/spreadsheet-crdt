import java.time.Instant
import java.util.*

data class OperationId(val sourceReplica: UUID, val timestamp: Instant, val sourceSequence: Long) : Comparable<OperationId> {
    override operator fun compareTo(other: OperationId): Int = compareValuesBy(this, other,
        { it.timestamp },
        { it.sourceReplica },
        { it.sourceSequence }
    )
}