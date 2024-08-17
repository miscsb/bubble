package com.miscsb.redis;

import com.miscsb.bubble.BubbleApplication;
import com.miscsb.bubble.TestConfig;
import com.miscsb.bubble.configuration.RedisConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootTest(classes = { BubbleApplication.class, RedisConfig.class })
@Import(TestConfig.class)
public class RedisProbabilisticTests {
    private static final Logger logger = LoggerFactory.getLogger(RedisProbabilisticTests.class);

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    // TEST GENERATION UTILS

    public enum CfOperationType { ADD, DEL }
    public record CfOperation(CfOperationType type, String value) {}

    private static final IntFunction<String> randomStringSupplier = (int size) -> {
        char[] buf = new char[size];
        for (int i = 0; i < size; i++) {
            buf[i] = (char) ('A' + Math.random() * ('Z' - 'A'));
        }
        return new String(buf);
    };

    static Stream<Arguments> parameterProviderTemplate(int keySize, int operations, double addDelRatio) {
        Function<String, CfOperation> opMapper = (String key) -> {
            CfOperationType type = Math.random() <= addDelRatio ? CfOperationType.ADD : CfOperationType.DEL;
            return new CfOperation(type, key);
        };
        Supplier<List<CfOperation>> argumentSupplier = () ->
                IntStream.generate(() -> keySize)
                        .limit(operations)
                        .mapToObj(randomStringSupplier)
                        .map(opMapper)
                        .toList();
        return Stream.generate(argumentSupplier).map(Arguments::of);
    }

    private static final double FP_THRESHOLD = 1E-2;
    private static final double FN_THRESHOLD = 5E-6;

    static void assertErrorRate(List<Boolean> expected, List<Boolean> actual) {
        Assertions.assertEquals(expected.size(), actual.size());

        int fp = 0; int fn = 0; int tp = 0; int tn = 0;
        for (int i = 0; i < actual.size(); i++) {
            boolean existsActual = actual.get(i);
            boolean existsExpected = expected.get(i);
            if ( existsExpected &&  existsActual) tp++;
            if ( existsExpected && !existsActual) fn++;
            if (!existsExpected &&  existsActual) fp++;
            if (!existsExpected && !existsActual) tn++;
        }

        double fpRate = (double) fp / (fp + tn);
        double fnRate = (double) fn / (fn + tp);
        logger.info("FP Rate: {}, FN Rate: {}", fpRate, fnRate);
        Assertions.assertTrue(Double.isNaN(fpRate) || fpRate <= FP_THRESHOLD, "False positive rate exceeds threshold");
        Assertions.assertTrue(Double.isNaN(fnRate) || fnRate <= FN_THRESHOLD, "False negative rate exceeds threshold");
    }

    // TESTS

    static Stream<Arguments> methodSourceTestErrorRate1() {
        return parameterProviderTemplate(5, 50_000, 0.5).limit(1);
    }

    @ParameterizedTest
    @MethodSource("methodSourceTestErrorRate1")
    public void testErrorRate(List<CfOperation> operations) {
        final String key = "TEST_ERROR_RATE_" + System.nanoTime();
        LettuceConnection connection = (LettuceConnection) redisConnectionFactory.getConnection();
        RedisProbabilisticCommands commands = RedisProbabilisticCommands.fromLettuceConnection(connection);

        connection.openPipeline();
        Map<String, Integer> freq = new HashMap<>();
        List<Boolean> expected = new ArrayList<>();

        commands.cfReserve(key, 100000);
        for (CfOperation operation : operations) {
            if (operation.type == CfOperationType.ADD) {
                commands.cfAdd(key, operation.value());
                freq.compute(operation.value(), (k, v) -> v == null ? 1 : v + 1);
            } else {
                commands.cfDel(key, operation.value());
                freq.compute(operation.value(), (k, v) -> v == null || v == 1 ? null : v - 1);
            }
            expected.add(freq.containsKey(operation.value()));
            commands.cfExists(key, operation.value());
        }

        List<Object> out = connection.closePipeline();
        List<Boolean> actual = IntStream.iterate(1, i -> i + 2)
                .limit(expected.size())
                .mapToObj(i -> (Boolean) out.get(i))
                .toList();
        assertErrorRate(expected, actual);
    }

    static Stream<Arguments> methodSourceTestCuckooFilter1() {
        return parameterProviderTemplate(5, 50, 0.5).limit(1);
    }

    @ParameterizedTest
    @MethodSource("methodSourceTestCuckooFilter1")
    public void testCuckooFilter(List<CfOperation> operations) {
        final String key = "TEST_CUCKOO_FILTER_" + System.nanoTime();
        RedisCuckooFilter filter = new RedisCuckooFilter(key, redisConnectionFactory);

        Map<String, Integer> freq = new HashMap<>();
        List<Boolean> expected = new ArrayList<>();
        List<Boolean> actual = new ArrayList<>();

        filter.reserve(100000);
        for (CfOperation operation : operations) {
            if (operation.type == CfOperationType.ADD) {
                filter.add(operation.value());
                freq.compute(operation.value(), (k, v) -> v == null ? 1 : v + 1);
            } else {
                filter.del(operation.value());
                freq.compute(operation.value(), (k, v) -> v == null || v == 1 ? null : v - 1);
            }
            expected.add(freq.containsKey(operation.value()));
            actual.add(filter.exists(operation.value()));
        }
        assertErrorRate(expected, actual);
    }

    @Test
    public void testCannotReserveTwice() {
        final String key = "TEST_RESERVE_TWICE_" + System.nanoTime();
        LettuceConnection connection = (LettuceConnection) redisConnectionFactory.getConnection();
        var commands = RedisProbabilisticCommands.fromLettuceConnection(connection);

        commands.cfReserve(key, 1000);
        Assertions.assertThrows(RedisSystemException.class, () -> commands.cfReserve(key, 1000));
    }

    @Test
    void contextLoads() {
    }

}
