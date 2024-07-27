package com.miscsb.bubble;

import java.util.function.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FunctionalUtils {

    public static final class Unit {
        private Unit() {
        }

        private static final Unit INSTANCE = new Unit();

        public static final Unit instance() {
            return INSTANCE;
        }

        public static final Unit instance(Object... xs) {
            return INSTANCE;
        }
    }

    public static Mono<Unit> optional(Mono<?> mono) {
        return Flux.concat(Mono.just(Unit.instance()), mono.map(Unit::instance)).last();
    }

    public static class SeqBuilder {
        private SeqBuilder() {}

        private Mono<Unit> val = Mono.just(Unit.instance());

        public SeqBuilder exec(Runnable runnable) {
            val = val.flatMap(FunctionalUtils.peek(runnable));
            return this;
        }

        public SeqBuilder exec(Mono<?> mono) {
            val = val.flatMap(FunctionalUtils.peek(mono));
            return this;
        }

        public SeqBuilder execMaybe(Mono<?> mono) {
            val = val.flatMap(FunctionalUtils.peek(FunctionalUtils.optional(mono)));
            return this;
        }

        public Mono<Unit> end() {
            return val;
        }
    }

    public static interface Seq {
        public static SeqBuilder start() {
            return new SeqBuilder();
        }
    }

    public static <T, U> Function<T, U> constant(U val) {
        return x -> val;
    }

    public static <T, U> Function<T, U> ifThenElse(Predicate<T> predicate, Function<T, U> onTrue,
            Function<T, U> onFalse) {
        return x -> predicate.test(x) ? onTrue.apply(x) : onFalse.apply(x);
    }

    public static <T> Function<T, Mono<T>> peek(Runnable runnable) {
        return t -> Mono.just(t).map(v -> { runnable.run(); return v; });
    }
    
    public static <T> Function<T, Mono<T>> peek(Consumer<T> consumer) {
        return t -> Mono.just(t).map(v -> { consumer.accept(v); return v; });
    }

    public static <T, U> Function<T, Mono<T>> peek(Mono<U> mono) {
        return t -> mono.map(constant(t));
    }

    public static <T> Function<T, Mono<T>> guard(Function<T, Mono<Boolean>> predicateM, Supplier<Exception> onFailM) {
        return x -> predicateM.apply(x).flatMap(result -> result ? Mono.just(x) : Mono.error(onFailM.get()));
    }

    public static <T> Function<T, Mono<Boolean>> negateM(Function<T, Mono<Boolean>> predicateM) {
        return x -> predicateM.apply(x).map(b -> !b);
    }

}
