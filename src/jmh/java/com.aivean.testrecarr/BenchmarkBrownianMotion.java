package com.aivean.testrecarr;

import com.aivean.recarr.RecordArray;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 2/* seconds */)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 4/* seconds */)
public class BenchmarkBrownianMotion {

/*
Benchmark                           (n)  (p2pCols)  (recImpl)  Mode  Cnt   Score   Error  Units
BenchmarkBrownianMotion.simulate  10000      false      false  avgt    5   0.053 ± 0.005  ms/op
BenchmarkBrownianMotion.simulate  10000      false       true  avgt    5   0.032 ± 0.001  ms/op
BenchmarkBrownianMotion.simulate  10000       true      false  avgt    5  71.456 ± 4.416  ms/op
BenchmarkBrownianMotion.simulate  10000       true       true  avgt    5  58.539 ± 1.728  ms/op
 */

    @Param({"10000"})
    public int n;

    @Param({"false", "true"})
    public boolean recImpl;

    @Param({"false", "true"})
    public boolean p2pCols;

    PointGameSimulator simulator;

    @Setup
    public void setup() {
        if (recImpl) {
            simulator = new PointGameSimulator() {
                final int nPoints = n;
                final RecordArray<PointGameSimulator.Point2D> points = RecordArray.create(Point2D.class, nPoints);

                @Override
                int getNumPoints() {
                    return nPoints;
                }

                @Override
                Point2D getPoint(int index) {
                    return points.get(index);
                }

                @Override
                void simulatePointToPointCollisions() {
                    if (p2pCols) {
                        super.simulatePointToPointCollisions();
                    }
                }
            };
        } else {
            simulator = new PointGameSimulator() {
                final int nPoints = n;
                final Point2D[] points = new Point2D[nPoints];

                {
                    for (int i = 0; i < nPoints; i++) {
                        points[i] = new Point2DImpl();
                    }
                }

                @Override
                int getNumPoints() {
                    return nPoints;
                }

                @Override
                Point2D getPoint(int index) {
                    return points[index];
                }

                @Override
                void simulatePointToPointCollisions() {
                    if (p2pCols) {
                        super.simulatePointToPointCollisions();
                    }
                }
            };
        }

        simulator.init();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        System.out.println("Point collisions: " + simulator.pointCollisions);
        System.out.println("Wall collisions: " + simulator.wallCollisions);
    }

    @Benchmark
    public void simulate() {
        simulator.simulate();
    }

    static class Point2DImpl implements PointGameSimulator.Point2D {
        float x, y, vx, vy;

        // default constructor
        public Point2DImpl() {
        }

        public Point2DImpl(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public void setX(float x) {
            this.x = x;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public void setY(float y) {
            this.y = y;
        }

        @Override
        public float getVx() {
            return vx;
        }

        @Override
        public void setVx(float vx) {
            this.vx = vx;
        }

        @Override
        public float getVy() {
            return vy;
        }

        @Override
        public void setVy(float vy) {
            this.vy = vy;
        }

        @Override
        public PointGameSimulator.Point2D copy() {
            return new Point2DImpl(x, y, vx, vy);
        }
    }
}
