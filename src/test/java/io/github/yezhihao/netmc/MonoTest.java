package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class MonoTest {

    private static final Logger log = LoggerFactory.getLogger(Session.class.getSimpleName());

    public static void main(String[] args) {
        Hooks.onErrorDropped(t -> log.warn("Reactor", t));

        Mono<Integer> mono = Mono.<Double>create(sink -> {
            log.warn("connect a");
            new Thread(() -> {
                log.warn("receive a");
                try {
                    double value = (1 / 1);
                    sink.success(value);
                } catch (Throwable e) {
                    sink.error(new Exception("calc error"));
                }
            }).start();
            sink.onDispose(() -> System.out.println("release a"));
        }).then(Mono.create(sink -> {
            log.warn("connect b");
            new Thread(() -> {
                log.warn("receive b");
                sink.success(1 + 1);
            }).start();
            sink.onDispose(() -> log.warn("release b"));
        }));

        mono.doOnSuccess(value -> log.warn("doOnSuccess1: " + value))
                .doOnSuccess(value -> log.warn("doOnSuccess2: " + value))
                .doOnError(throwable -> log.warn("doOnError"))
                .doFinally(signalType -> log.warn("doFinally"))
                .subscribe();
    }
}
