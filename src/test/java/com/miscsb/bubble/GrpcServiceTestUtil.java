package com.miscsb.bubble;

import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class GrpcServiceTestUtil {
    public static <T, U> U callOne(BiConsumer<T, StreamObserver<U>> function, T request) throws StatusException {
        List<U> result = new ArrayList<>();
        Throwable[] error = new Throwable[1];
        StreamObserver<U> observer = new StreamObserver<U>() {
            @Override
            public void onNext(U value) {
                result.add(value);
            }
            @Override
            public void onError(Throwable t) {
                error[0] = t;
            }
            @Override
            public void onCompleted() {
            }
        };
        function.accept(request, observer);
        if (error[0] != null) {
            throw new StatusException(io.grpc.Status.fromThrowable(error[0]));
        }
        if (result.size() != 1) {
            throw new RuntimeException("Expected 1 result, got " + result.size());
        }
        return result.getFirst();
    }

    public static <T, U> List<U> callMany(BiConsumer<T, StreamObserver<U>> function, T request) throws StatusException {
        List<U> result = new ArrayList<>();
        Throwable[] error = new Throwable[1];
        StreamObserver<U> observer = new StreamObserver<U>() {
            @Override
            public void onNext(U value) {
                result.add(value);
            }
            @Override
            public void onError(Throwable t) {
                error[0] = t;
            }
            @Override
            public void onCompleted() {
            }
        };
        function.accept(request, observer);
        if (error[0] != null) {
            throw new StatusException(io.grpc.Status.fromThrowable(error[0]));
        }
        return result;
    }

    public static <T, U> Optional<U> callOptional(BiConsumer<T, StreamObserver<U>> function, T request) throws StatusException {
        List<U> result = new ArrayList<>();
        Throwable[] error = new Throwable[1];
        StreamObserver<U> observer = new StreamObserver<U>() {
            @Override
            public void onNext(U value) {
                result.add(value);
            }
            @Override
            public void onError(Throwable t) {
                error[0] = t;
            }
            @Override
            public void onCompleted() {
            }
        };
        function.accept(request, observer);
        if (error[0] != null) {
            throw new StatusException(io.grpc.Status.fromThrowable(error[0]));
        }
        if (result.size() > 1) {
            throw new RuntimeException("Expected 0 or 1 result, got " + result.size());
        }
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }
}
