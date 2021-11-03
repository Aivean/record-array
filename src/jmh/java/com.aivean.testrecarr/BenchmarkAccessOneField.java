package com.aivean.testrecarr;

import com.aivean.recarr.RecordArray;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.aivean.testrecarr.BenchUtils.getIndexer;
import static com.aivean.testrecarr.BenchUtils.shuffleArray;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 2/* seconds */)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Measurement(iterations = 5, time = 2/* seconds */)
public class BenchmarkAccessOneField {

    @Param({"100000"})
    public int n;

    @Param({"false", "true"})
    public boolean shuffleCls;

    @Param({"false", "true"})
    public boolean rngAccess;

/*
Benchmark                                (n)  (rngAccess)  (shuffleCls)  Mode  Cnt        Score         Error  Units
BenchmarkAccessOneField.classArrGet   100000        false         false  avgt    5   312322.361 ±   56636.230  ns/op
BenchmarkAccessOneField.classArrGet   100000        false          true  avgt    5   492894.887 ±   35842.178  ns/op
BenchmarkAccessOneField.classArrGet   100000         true         false  avgt    5  1876402.804 ±  239886.392  ns/op
BenchmarkAccessOneField.classArrGet   100000         true          true  avgt    5  2713050.609 ± 1398322.621  ns/op
BenchmarkAccessOneField.classArrSet   100000        false         false  avgt    5    78360.832 ±    1034.406  ns/op
BenchmarkAccessOneField.classArrSet   100000        false          true  avgt    5   183640.110 ±    8232.544  ns/op
BenchmarkAccessOneField.classArrSet   100000         true         false  avgt    5  1027455.024 ±   54722.563  ns/op
BenchmarkAccessOneField.classArrSet   100000         true          true  avgt    5  1035971.792 ±   10323.835  ns/op
BenchmarkAccessOneField.intArrGet     100000        false         false  avgt    5   296363.542 ±   20275.233  ns/op
BenchmarkAccessOneField.intArrGet     100000        false          true  avgt    5   304254.954 ±   58351.720  ns/op
BenchmarkAccessOneField.intArrGet     100000         true         false  avgt    5  1269768.390 ±   71712.690  ns/op
BenchmarkAccessOneField.intArrGet     100000         true          true  avgt    5  1219701.456 ±   23165.319  ns/op
BenchmarkAccessOneField.intArrSet     100000        false         false  avgt    5    34023.947 ±    1630.673  ns/op
BenchmarkAccessOneField.intArrSet     100000        false          true  avgt    5    34681.582 ±     270.818  ns/op
BenchmarkAccessOneField.intArrSet     100000         true         false  avgt    5   859999.014 ±    8685.462  ns/op
BenchmarkAccessOneField.intArrSet     100000         true          true  avgt    5   857579.410 ±   32818.019  ns/op
BenchmarkAccessOneField.noopBaseline  100000        false         false  avgt    5   233993.926 ±     688.871  ns/op
BenchmarkAccessOneField.noopBaseline  100000        false          true  avgt    5   236628.301 ±    6888.255  ns/op
BenchmarkAccessOneField.noopBaseline  100000         true         false  avgt    5  1075476.007 ±   28655.696  ns/op
BenchmarkAccessOneField.noopBaseline  100000         true          true  avgt    5  1071452.716 ±   45468.794  ns/op
BenchmarkAccessOneField.recArrGet     100000        false         false  avgt    5   333413.362 ±   12529.800  ns/op
BenchmarkAccessOneField.recArrGet     100000        false          true  avgt    5   328531.670 ±    1778.968  ns/op
BenchmarkAccessOneField.recArrGet     100000         true         false  avgt    5  1212782.807 ±    5480.539  ns/op
BenchmarkAccessOneField.recArrGet     100000         true          true  avgt    5  1222654.720 ±   32847.829  ns/op
BenchmarkAccessOneField.recArrSet     100000        false         false  avgt    5    33726.757 ±     233.818  ns/op
BenchmarkAccessOneField.recArrSet     100000        false          true  avgt    5    33831.184 ±    1493.991  ns/op
BenchmarkAccessOneField.recArrSet     100000         true         false  avgt    5   872252.180 ±   20231.116  ns/op
BenchmarkAccessOneField.recArrSet     100000         true          true  avgt    5  1060525.166 ±  506994.614  ns/op
 */

    static class Point {
        int x;
        int y;
    }

    RecordArray<SimpleRecord> recArr;
    Point[] classArr;
    int[] intArr;

    BenchUtils.Indexer indexer;

    @Setup
    public void setup() {
        recArr = RecordArray.create(SimpleRecord.class, n);
        classArr = new Point[n];
        for (int i = 0; i < n; i++) {
            classArr[i] = new Point();
        }
        if (shuffleCls) {
            shuffleArray(classArr);
        }
        intArr = new int[n];

        indexer = getIndexer(rngAccess, n);
    }

    @Benchmark
    public void noopBaseline(Blackhole bh) {
        for (int i = 0; i < n; i++) {
            bh.consume(indexer.getIndex(i));
        }
    }

    @Benchmark
    public void recArrSet() {
        for (int i = 0; i < n; i++) {
            recArr.get(indexer.getIndex(i)).setAge(i);
        }
    }

    @Benchmark
    public void recArrGet(Blackhole bh) {
        for (int i = 0; i < n; i++) {
            bh.consume(recArr.get(indexer.getIndex(i)).getAge());
        }
    }

    @Benchmark
    public void classArrSet() {
        for (int i = 0; i < n; i++) {
            classArr[indexer.getIndex(i)].x = i;
        }
    }

    @Benchmark
    public void classArrGet(Blackhole bh) {
        for (int i = 0; i < n; i++) {
            bh.consume(classArr[indexer.getIndex(i)].x);
        }
    }

    @Benchmark
    public void intArrSet() {
        for (int i = 0; i < n; i ++) {
            intArr[indexer.getIndex(i)] = i;
        }
    }

    @Benchmark
    public void intArrGet(Blackhole bh) {
        for (int i = 0; i < n; i ++) {
            bh.consume(intArr[indexer.getIndex(i)]);
        }
    }
}
