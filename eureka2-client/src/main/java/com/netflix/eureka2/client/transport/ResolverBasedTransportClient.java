package com.netflix.eureka2.client.transport;

import com.netflix.eureka2.client.resolver.ServerResolver;
import com.netflix.eureka2.config.EurekaTransportConfig;
import com.netflix.eureka2.transport.MessageConnection;
import com.netflix.eureka2.transport.TransportClient;
import com.netflix.eureka2.transport.base.BaseMessageConnection;
import com.netflix.eureka2.transport.base.HeartBeatConnection;
import com.netflix.eureka2.metric.MessageConnectionMetrics;
import com.netflix.eureka2.transport.base.SelfClosingConnection;
import com.netflix.eureka2.Server;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Convenience base implementation for {@link com.netflix.eureka2.transport.TransportClient} that reads the server list from a {@link ServerResolver}
 *
 * @author Nitesh Kant
 */
public abstract class ResolverBasedTransportClient implements TransportClient {

    private static final Logger logger = LoggerFactory.getLogger(ResolverBasedTransportClient.class);

    private final String clientId;
    private final AtomicInteger clientInstanceIdx = new AtomicInteger();
    private final EurekaTransportConfig config;
    private final ServerResolver resolver;
    private final PipelineConfigurator<Object, Object> pipelineConfigurator;
    private final MessageConnectionMetrics metrics;
    private final ConcurrentHashMap<Server, RxClient<Object, Object>> clients;

    protected ResolverBasedTransportClient(String clientId,
                                           EurekaTransportConfig config,
                                           ServerResolver resolver,
                                           PipelineConfigurator<Object, Object> pipelineConfigurator,
                                           MessageConnectionMetrics metrics) {
        this.clientId = clientId;
        this.config = config;
        this.resolver = resolver;
        this.pipelineConfigurator = pipelineConfigurator;
        this.metrics = metrics;
        clients = new ConcurrentHashMap<>();
    }

    @Override
    public Observable<MessageConnection> connect() {
        return resolver.resolve()
                .take(1)
                .map(new Func1<Server, RxClient<Object, Object>>() {
                    @Override
                    public RxClient<Object, Object> call(Server server) {
                        // This should be invoked from a single thread.
                        RxClient<Object, Object> client = clients.get(server);
                        if (null == client) {
                            client = RxNetty.createTcpClient(server.getHost(), server.getPort(),
                                    pipelineConfigurator);
                            clients.put(server, client);
                        }

                        logger.info("Connecting to server {}", server);
                        return client;
                    }
                })
                .flatMap(new Func1<RxClient<Object, Object>, Observable<MessageConnection>>() {
                    @Override
                    public Observable<MessageConnection> call(final RxClient<Object, Object> client) {
                        return client.connect()
                                .map(new Func1<ObservableConnection<Object, Object>, MessageConnection>() {
                                    @Override
                                    public MessageConnection call(
                                            ObservableConnection<Object, Object> conn) {
                                        String clientInstanceId = clientId + '#' + clientInstanceIdx.incrementAndGet();
                                        return new SelfClosingConnection(
                                                new HeartBeatConnection(
                                                        new BaseMessageConnection(clientInstanceId, conn, metrics),
                                                            config.getHeartbeatIntervalMs(), 3, Schedulers.computation()
                                                ),
                                                config.getConnectionAutoTimeoutMs()
                                        );
                                    }
                                });
                    }
                });
    }

    @Override
    public void shutdown() {
        for (RxClient<Object, Object> client : clients.values()) {
            client.shutdown();
        }
    }
}