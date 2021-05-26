package com.netflix.eureka.cluster;

import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 同步task任务
 *
 * Base class for all replication tasks.
 */
abstract class ReplicationTask {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationTask.class);

    /**
     * 目标server的名称（节点路径）
     */
    protected final String peerNodeName;
    /**
     * 任务操作的类型
     */
    protected final Action action;

    ReplicationTask(String peerNodeName, Action action) {
        this.peerNodeName = peerNodeName;
        this.action = action;
    }

    public abstract String getTaskName();

    public Action getAction() {
        return action;
    }

    public abstract EurekaHttpResponse<?> execute() throws Throwable;

    /**
     * 处理成功的回调
     */
    public void handleSuccess() {
    }

    /**
     * 处理失败的回调
     *
     * @param statusCode
     * @param responseEntity
     * @throws Throwable
     */
    public void handleFailure(int statusCode, Object responseEntity) throws Throwable {
        logger.warn("The replication of task {} failed with response code {}", getTaskName(), statusCode);
    }
}
