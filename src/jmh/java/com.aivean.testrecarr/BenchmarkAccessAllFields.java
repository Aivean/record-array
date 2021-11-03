package com.aivean.testrecarr;

import com.aivean.recarr.RecordArray;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 2/* seconds */)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Measurement(iterations = 5, time = 2/* seconds */)
public class BenchmarkAccessAllFields {

/*
 Benchmark                                 (n)  (rngAccess)  (shuffleCls)  Mode  Cnt        Score         Error  Units
 BenchmarkAccessAllFields.classArrGet   100000        false         false  avgt    5   417680.819 ±  235336.237  ns/op
 BenchmarkAccessAllFields.classArrGet   100000        false          true  avgt    5   801033.810 ±  749473.582  ns/op
 BenchmarkAccessAllFields.classArrGet   100000         true         false  avgt    5  2197091.902 ±  142150.925  ns/op
 BenchmarkAccessAllFields.classArrGet   100000         true          true  avgt    5  3832137.898 ± 4449372.216  ns/op
 BenchmarkAccessAllFields.classArrSet   100000        false         false  avgt    5   206402.825 ±    6026.232  ns/op
 BenchmarkAccessAllFields.classArrSet   100000        false          true  avgt    5   350456.561 ±   24691.727  ns/op
 BenchmarkAccessAllFields.classArrSet   100000         true         false  avgt    5  1147442.232 ±   76316.824  ns/op
 BenchmarkAccessAllFields.classArrSet   100000         true          true  avgt    5  1139283.510 ±   42277.691  ns/op
 BenchmarkAccessAllFields.intArrGet     100000        false         false  avgt    5   459436.492 ±    9240.779  ns/op
 BenchmarkAccessAllFields.intArrGet     100000        false          true  avgt    5   458810.597 ±   12815.743  ns/op
 BenchmarkAccessAllFields.intArrGet     100000         true         false  avgt    5  1729598.334 ±   55391.651  ns/op
 BenchmarkAccessAllFields.intArrGet     100000         true          true  avgt    5  1726518.268 ±  116982.319  ns/op
 BenchmarkAccessAllFields.intArrSet     100000        false         false  avgt    5   201216.948 ±   14507.019  ns/op
 BenchmarkAccessAllFields.intArrSet     100000        false          true  avgt    5   199857.833 ±    6582.596  ns/op
 BenchmarkAccessAllFields.intArrSet     100000         true         false  avgt    5   992211.816 ±   44317.492  ns/op
 BenchmarkAccessAllFields.intArrSet     100000         true          true  avgt    5   992275.134 ±   41632.019  ns/op
 BenchmarkAccessAllFields.noopBaseline  100000        false         false  avgt    5   256589.456 ±   14145.265  ns/op
 BenchmarkAccessAllFields.noopBaseline  100000        false          true  avgt    5   259359.594 ±   12865.678  ns/op
 BenchmarkAccessAllFields.noopBaseline  100000         true         false  avgt    5  1156316.057 ±   36088.628  ns/op
 BenchmarkAccessAllFields.noopBaseline  100000         true          true  avgt    5  1150244.783 ±   41072.102  ns/op
 BenchmarkAccessAllFields.recArrGet     100000        false         false  avgt    5   482345.510 ±   12947.381  ns/op
 BenchmarkAccessAllFields.recArrGet     100000        false          true  avgt    5   483027.557 ±   19817.451  ns/op
 BenchmarkAccessAllFields.recArrGet     100000         true         false  avgt    5  1729590.939 ±   70038.557  ns/op
 BenchmarkAccessAllFields.recArrGet     100000         true          true  avgt    5  1731700.374 ±   80179.461  ns/op
 BenchmarkAccessAllFields.recArrSet     100000        false         false  avgt    5   199301.525 ±    4958.942  ns/op
 BenchmarkAccessAllFields.recArrSet     100000        false          true  avgt    5   200073.725 ±   10791.072  ns/op
 BenchmarkAccessAllFields.recArrSet     100000         true         false  avgt    5   986441.601 ±   30047.679  ns/op
 BenchmarkAccessAllFields.recArrSet     100000         true          true  avgt    5   970269.651 ±   31591.067  ns/op
 */

    @Param({"100000"})
    public int n;

    @Param({"false", "true"})
    public boolean shuffleCls;

    @Param({"false", "true"})
    public boolean rngAccess;

    static class Point {
        int x;
        int y;
        int z;

        int getAll() {
            return x ^ y ^ z;
        }

        void setAll(int i) {
            x = y = z = i;
        }
    }

    RecordArray<PointRecord> recArr;
    Point[] classArr;
    int[] intArrX;
    int[] intArrY;
    int[] intArrZ;

    BenchUtils.Indexer indexer;

    @Setup
    public void setup() {
        recArr = RecordArray.create(PointRecord.class, n);
        classArr = new Point[n];
        for (int i = 0; i < n; i++) {
            classArr[i] = new Point();
        }
        if (shuffleCls) {
            BenchUtils.shuffleArray(classArr);
        }
        intArrX = new int[n];
        intArrY = new int[n];
        intArrZ = new int[n];

        indexer = BenchUtils.getIndexer(rngAccess, n);
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
            recArr.get(indexer.getIndex(i)).setAll(i);
        }
    }

    @Benchmark
    public void recArrGet(Blackhole bh) {
        for (int i = 0; i < n; i++) {
            bh.consume(recArr.get(indexer.getIndex(i)).getAll());
        }
    }

    @Benchmark
    public void classArrSet() {
        for (int i = 0; i < n; i++) {
            classArr[indexer.getIndex(i)].setAll(i);
        }
    }

    @Benchmark
    public void classArrGet(Blackhole bh) {
        for (int i = 0; i < n; i++) {
            bh.consume(classArr[indexer.getIndex(i)].getAll());
        }
    }

    @Benchmark
    public void intArrSet() {
        for (int i = 0; i < n; i++) {
            intArrX[indexer.getIndex(i)] = i;
            intArrY[indexer.getIndex(i)] = i;
            intArrZ[indexer.getIndex(i)] = i;
        }
    }

    @Benchmark
    public void intArrGet(Blackhole bh) {
        for (int i = 0; i < n; i++) {
            int idx = indexer.getIndex(i);
            bh.consume(intArrX[idx] ^ intArrY[idx] ^ intArrZ[idx]);
        }
    }
}
