package com.netflix.eureka.util.batcher;

import java.util.List;

/**
 * 任务执行器
 *
 * An interface to be implemented by clients for task execution.
 *
 * @author Tomasz Bak
 */
public interface TaskProcessor<T> {

    /**
     * A processed task/task list ends up in one of the following states:
     * <ul>
     *     <li>{@code Success} processing finished successfully</li>
     *     <li>{@code TransientError} processing failed, but shall be retried later</li>
     *     <li>{@code PermanentError} processing failed, and is non recoverable</li>
     * </ul>
     */
    enum ProcessingResult {
        //成功
        Success,
        //拥堵
        Congestion,
        //短暂错误
        TransientError,
        //永久错误
        PermanentError
    }

    /**
     * In non-batched mode a single task is processed at a time.
     */
    ProcessingResult process(T task);

    /**
     * For batched mode a collection of tasks is run at a time. The result is provided for the aggregated result,
     * and all tasks are handled in the same way according to what is returned (for example are rescheduled, if the
     * error is transient).
     */
    ProcessingResult process(List<T> tasks);
}
