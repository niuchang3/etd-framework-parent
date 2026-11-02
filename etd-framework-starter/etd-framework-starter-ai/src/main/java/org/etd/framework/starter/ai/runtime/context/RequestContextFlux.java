package org.etd.framework.starter.ai.runtime.context;

import org.reactivestreams.Subscription;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot.Scope;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

/** 请求作用域覆盖原生流的订阅、信号、背压和取消；每个同步边界退出即恢复原线程上下文。 */
public final class RequestContextFlux<T> extends Flux<T> {
    private final Flux<T> source;
    private final RequestContextSnapshot snapshot;

    /** @param source 官方流，不主动订阅或建立第二个缓冲桥 @param snapshot 本次 Run 的可信身份快照 */
    public RequestContextFlux(Flux<T> source, RequestContextSnapshot snapshot) {
        this.source = source;
        this.snapshot = snapshot;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        ScopedSubscriber<T> subscriber = new ScopedSubscriber<>(actual, snapshot);
        try (Scope scope = snapshot.openScope()) {
            source.subscribe(subscriber);
        }
    }

    /** 作用域只覆盖当前线程的一次操作，不跨异步等待保留 ThreadLocal。 */
    private static final class ScopedSubscriber<T> implements CoreSubscriber<T>, Subscription {
        private final CoreSubscriber<? super T> actual;
        private final RequestContextSnapshot snapshot;
        private Subscription subscription;

        private ScopedSubscriber(CoreSubscriber<? super T> actual, RequestContextSnapshot snapshot) {
            this.actual = actual;
            this.snapshot = snapshot;
        }

        @Override
        public Context currentContext() {
            return actual.currentContext();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            try (Scope scope = snapshot.openScope()) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T value) {
            try (Scope scope = snapshot.openScope()) {
                actual.onNext(value);
            }
        }

        @Override
        public void onError(Throwable failure) {
            try (Scope scope = snapshot.openScope()) {
                actual.onError(failure);
            }
        }

        @Override
        public void onComplete() {
            try (Scope scope = snapshot.openScope()) {
                actual.onComplete();
            }
        }

        @Override
        public void request(long count) {
            try (Scope scope = snapshot.openScope()) {
                subscription.request(count);
            }
        }

        @Override
        public void cancel() {
            try (Scope scope = snapshot.openScope()) {
                subscription.cancel();
            }
        }
    }
}
