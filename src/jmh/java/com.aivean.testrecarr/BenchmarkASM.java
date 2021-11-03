package com.aivean.testrecarr;

import com.aivean.recarr.RecordArray;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.DTraceAsmProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(value = 1
        , jvmArgs = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly"}
)
@Warmup(iterations = 5, time = 2/* seconds */)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Measurement(iterations = 5, time = 2/* seconds */)
public class BenchmarkASM {

    @Param({"100000"})
    public int n;

    RecordArray<SimpleRecord> recArr;
    RecordArray<PointRecord> pointRecArr;
    int[] intArr;
    Point[] classArr;

    int i = 0;

    static class Point {
        int x;
        int y;
    }

    @Setup
    public void setup() {
        recArr = RecordArray.create(SimpleRecord.class, n);
        pointRecArr = RecordArray.create(PointRecord.class, n);
        intArr = new int[n];
        classArr = new Point[n];
        for (int i = 0; i < n; i++) {
            classArr[i] = new Point();
        }
    }

    @Benchmark
    public void noop(Blackhole bh) {
        bh.consume(i);
        i = (++i) % n;
    }

    @Benchmark
    public void pointRecArrGet(Blackhole bh) {
        bh.consume(pointRecArr.get(i).getX());
        i = (++i) % n;
    }

    @Benchmark
    public void recArrGet(Blackhole bh) {
        bh.consume(recArr.get(i).getAge());
        i = (++i) % n;
    }

    @Benchmark
    public void intArrGet(Blackhole bh) {
        bh.consume(intArr[i]);
        i = (++i) % n;
    }

    @Benchmark
    public void classArrGet(Blackhole bh) {
        bh.consume(classArr[i].x);
        i = (++i) % n;
    }

    @Benchmark
    public void pointRecArrSet(Blackhole bh) {
        pointRecArr.get(i).setX(i);
        i = (++i) % n;
    }

    @Benchmark
    public void intArrSet() {
        intArr[i] = i;
        i = (++i) % n;
    }

    @Benchmark
    public void recArrSet(Blackhole bh) {
        recArr.get(i).setAge(i);
        i = (++i) % n;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + BenchmarkASM.class.getSimpleName() + ".*")
                .forks(1)
                .verbosity(VerboseMode.EXTRA)
                .addProfiler(DTraceAsmProfiler.class)
                .build();

        new Runner(opt).run();
    }
}
