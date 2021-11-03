# Record Arrays

A small library that provides fast and memory-efficient array of "records" 
implementation for Java and Kotlin, using [parallel arrays](https://en.wikipedia.org/wiki/Parallel_array) 
(SoA, structure of arrays) memory layout.

**Warning**: This is a (working) prototype. Use at your own risk.

## Short description

This library uses annotation processing to find interfaces, marked with
`@Record` annotation, and generates an implementation of the `RecordArray` for each of them.

[Record Array](record-array/src/main/java/com/aivean/recarr/RecordArray.java)
provides several methods that mimic the "interface" of the regular array of objects.

For example, `T RecordArray<T>::get(int index)` returns the "record" at the given index.
The returned "record" is an interface `T` , backed by a generated private class, which
internally holds the reference to the `RecordArray` and `int` index.

When getters and setters of such record are invoked, they delegate access
to the corresponding parallel array fields inside the `RecordArray` at the given index.

```java
@Record
interface Point {
    int getX();
    int getY();
    void setX(int x);
    void setY(int y);
}

RecordArray<Point> points = RecordArray.create(Point.class, 10);
Point p = points.get(0);
p.setX(1);
p.setY(2);
System.out.println(p.getX() + " " + p.getY());
```


## Documentation:

1. [Motivation](doc/motivation.md)
2. [Usage](doc/usage.md)
3. [Quirks](doc/quirks.md)
4. [Internals](doc/internals.md)
5. [Performance](doc/performance.md)
6. [Future work](doc/todo.md)

## Building

This repository consists of the outer "harness" with tests and 
benchmarks, and the inner `record-array` module, that contains
the annotation processor and `RecordArray` interface. 
The inner module is packed into a dependency-free library and can be published.

```bash
# Build everything
./gradlew clean build 

# Run tests
./gradlew test

# Run all benchmarks (except BenchmarkASM)
./gradlew jmh

# Run specific benchmarks (regex)
./gradlew jmh -PjmhInclude=.*BenchmarkBrownianMotion.*

```

To profile assembly from `BenchmarkASM`: 

On linux:
1. change `addProfiler` inside `BenchmarkASM.java` to your profiler 
(probably `LinuxPerfAsmProfiler.class`)
2. build jar with `./gradlew jmhJar`
3. run `java -jar build/libs/record-array-harness-1.-SNAPSHOT-jmh.jar com.aivean.testrecarr.BenchmarkASM -prof perfasm > log.txt`

On mac:
1. build jar with `./gradlew jmhJar`
2. ensure that you have `hsdis-amd64.dylib` lib. If not, [download it](https://github.com/a10y/hsdis-macos). 
3. run `LD_LIBRARY_PATH=/path/to/hdis/ java -jar build/libs/record-array-harness-1.-SNAPSHOT-jmh.jar com.aivean.testrecarr.BenchmarkASM -prof dtraceasm > log.txt`  


## Licence

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
