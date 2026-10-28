package benchmark;

/**
 * Mutable collector for operations that the existing algorithms actually
 * perform. Create one collector per execution; snapshots are immutable.
 */
public final class OperationCounter {
    private long edgeExaminations;
    private long priorityQueuePolls;
    private long priorityQueueInsertions;
    private long successfulRelaxations;
    private long unionAttempts;
    private long successfulUnions;
    private long verticesProcessed;
    private long verticesVisited;

    public void recordEdgeExamination() {
        edgeExaminations++;
    }

    public void recordPriorityQueuePoll() {
        priorityQueuePolls++;
    }

    public void recordPriorityQueueInsertion() {
        priorityQueueInsertions++;
    }

    public void recordSuccessfulRelaxation() {
        successfulRelaxations++;
    }

    public void recordUnionAttempt() {
        unionAttempts++;
    }

    public void recordSuccessfulUnion() {
        successfulUnions++;
    }

    public void recordVertexProcessed() {
        verticesProcessed++;
    }

    public void recordVertexVisit() {
        verticesVisited++;
    }

    public OperationCounts snapshot() {
        return new OperationCounts(edgeExaminations, priorityQueuePolls,
                priorityQueueInsertions, successfulRelaxations, unionAttempts,
                successfulUnions, verticesProcessed, verticesVisited);
    }

    /**
     * Immutable snapshot of measured operation counts. Counters not applicable
     * to an algorithm are zero.
     */
    public record OperationCounts(
            long edgeExaminations,
            long priorityQueuePolls,
            long priorityQueueInsertions,
            long successfulRelaxations,
            long unionAttempts,
            long successfulUnions,
            long verticesProcessed,
            long verticesVisited) {
        public OperationCounts {
            if (edgeExaminations < 0 || priorityQueuePolls < 0
                    || priorityQueueInsertions < 0 || successfulRelaxations < 0
                    || unionAttempts < 0 || successfulUnions < 0
                    || verticesProcessed < 0 || verticesVisited < 0) {
                throw new IllegalArgumentException("Operation counts cannot be negative");
            }
        }
    }
}
