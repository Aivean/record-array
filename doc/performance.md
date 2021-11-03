# Performance

Warning: this project is neither a silver bullet, 
nor a drop-in replacement for arrays of objects. Read carefully about the tradeoffs below.

Before reading this section, please check the
[Motivation](motivation.md), [Usage](usage.md), and [Internals](internals.md).

---

## Short summary

This project shines, when:

* there is a large (> 1000 elements) array of simple homogeneous objects
* objects are processed sequentially in large batches
* only one or few fields are accessed during processing
* objects mostly have primitive fields
* a small memory footprint is required

This project won't help, when:
* array consists of a small number of large objects
* array is accessed randomly and infrequently
* fields of the array elements are accessed repeatedly during processing
   (CPU instructions to access the fields dominate the memory accesses)
* processing logic is long and complex 
  (may prevent method inlining and other optimizations)

----

## Important notes

As can be seen in [Internals](internals.md), for every array access:

```java
arr.get(i)
```
a new object (`$$Record`) is created:
```java
public $$Record get(int i) {
    return new $$Record(i);
}
```

This may seem like an expensive operation, but in reality it is not. 
JIT eliminates the object creation and inlines method calls.

Here is the ASM produce by JIT for the following benchmark:

```java
// RecordArray<SimpleRecord> recArr;
@Benchmark
public void recArrGet(Blackhole bh) {
    bh.consume(recArr.get(i).getAge());
    i = (++i) % n;
}
```
<sup>Java HotSpot(TM) 64-Bit Server VM (build 15.0.2+7-27, mixed mode, sharing)</sup>


```asm
      mov    $0x1,%ebp               ; ebp = 1
      test   %r10d,%r10d             ; if control.isDone      (jmh internals)
╭     jne    0x0000000120be1873      ; goto 0x120be1873       ;*ifeq {reexecute=0 rethrow=0 return_oop=0}
│                                    ;                        ; - recArrGet_avgt_jmhStub@30 (line 188)
│     xchg   %ax,%ax                 ; nop                    ;*aload_1 {reexecute=0 rethrow=0 return_oop=0}
│                                    ;                        ; - recArrGet_avgt_jmhStub@33 (line 189)
│  ↗  mov    0x50(%rsp),%r10         ; r10 = this             ;*aload_0 {reexecute=0 rethrow=0 return_oop=0}
│  │  mov    0x14(%r10),%r8d         ; r8d = recArr           ;*getfield recArr {reexecute=0 rethrow=0 return_oop=0}
│  │                                 ;                        ; - BenchmarkASM::recArrGet@2 (line 68)
│  │                                 ;                        ; - recArrGet_avgt_jmhStub@17 (line 186)
│  │  mov    0x10(%r10),%r10d        ; r10d = i               ;*getfield i {reexecute=0 rethrow=0 return_oop=0}
│  │                                 ;                        ; - BenchmarkASM::recArrGet@6 (line 68)
│  │                                 ;                        ; - recArrGet_avgt_jmhStub@17 (line 186)
│  │  mov    0x8(%r12,%r8,8),%r11d   ; r11d = recArr metadata ; implicit exception: dispatches to 0x0000000120be1954
│  │  data16 nopw 0x0(%rax,%rax,1)   ; nopw                
│  │  data16 data16 xchg %ax,%ax     ; nopw                
│  │  cmp    $0x17736d,%r11d         ; if (r11d == 17736d)    ; {metadata('RecordArrayFactoryImpl$SimpleRecordImpl$1$7388')}
│  │  jne    0x0000000120be18d8      ; goto 0x120be18d8       ;*invokespecial <init> {reexecute=0 rethrow=0 return_oop=0}
│  │                                 ;                        ; - RecordArrayFactoryImpl$SimpleRecordImpl$1$7388::get@6 (line 264)
│  │                                 ;                        ; - RecordArrayFactoryImpl$SimpleRecordImpl$1$7388::get@2 (line 225)
│  │                                 ;                        ; - BenchmarkASM::recArrGet@9 (line 68)
│  │                                 ;                        ; - recArrGet_avgt_jmhStub@17 (line 186)
│  │  lea    (%r12,%r8,8),%r11       ; r11 = &recArr          ; implicit exception: dispatches to 0x0000000120be1954
│  │                                 ;                        ; - BenchmarkASM::recArrGet@9 (line 68)
│  │                                 ;                        ; - recArrGet_avgt_jmhStub@17 (line 186)
│  │  mov    0x1c(%r11),%r8d         ; r8d = recArr.Age       ;*getfield Age {reexecute=0 rethrow=0 return_oop=0}
│  │                                 ;                        ; - RecordArrayFactoryImpl$SimpleRecordImpl$1$7388$$$Record::getAge@4 (line 300)
│  │                                 ;                        ; - BenchmarkASM::recArrGet@17 (line 68)
│  │                                 ;                        ; - recArrGet_avgt_jmhStub@17 (line 186)
│  │  mov    0xc(%r12,%r8,8),%r9d    ; r9d = Age.length       ; implicit exception: dispatches to 0x0000000120be1964
│  │  nopw   0x0(%rax,%rax,1)        ; nopw                
│  │  cmp    %r9d,%r10d              ; if (Age.length <= i) 
│  │  jae    0x0000000120be18b5      ; goto 0x120be18b5
│  │  lea    (%r12,%r8,8),%r11       ; r11 = &Age
│  │  mov    0x10(%r11,%r10,4),%edx  ; edx = Age[i]           ;*iaload {reexecute=0 rethrow=0 return_oop=0}
│  │                                 ;                        ; - RecordArrayFactoryImpl$SimpleRecordImpl$1$7388$$$Record::getAge@11 (line 300)
│  │                                 ;                        ; - BenchmarkASM::recArrGet@17 (line 68)
│  │                                 ;                        ; - recArrGet_avgt_jmhStub@17 (line 186)
│  │  mov    0x10(%rsp),%rsi         ;                 
│  │  callq  0x0000000119133400      ;                        ; ImmutableOopMap {[64]=Oop [72]=Oop [80]=Oop [16]=Oop }
│  │                                 ;                               ;*invokespecial consumeFull {reexecute=0 rethrow=0 return_oop=0}
```
(Note: listing is cut right before the code, corresponding to `i = (++i) % n`)

There are some metadata checks, but there are no method calls or allocations.

Still, the same code for array of objects is much shorter:
```java
// Point[] classArr;
@Benchmark
public void classArrGet(Blackhole bh) {
    bh.consume(classArr[i].x);
    i = (++i) % n;
}
```


```asm

      mov    $0x1,%ebp               ; ebp = 1
      test   %r11d,%r11d             ; if control.isDone  (jmh internals)
╭     jne    0x000000011d0f0646      ;                        ;*ifeq {reexecute=0 rethrow=0 return_oop=0}
│                                    ;                        ; - classArrGet_avgt_jmhStub@30 (line 188)
│     nopl   0x0(%rax,%rax,1)        ; nopw                   ;*aload_1 {reexecute=0 rethrow=0 return_oop=0}
│                                    ;                        ; - classArrGet_avgt_jmhStub@33 (line 189)
│  ↗  mov    0x50(%rsp),%r10         ; r10 = this
│  │  mov    0x1c(%r10),%r10d        ; r10d = this.classArr   ;*getfield classArr {reexecute=0 rethrow=0 return_oop=0}
│  │                                 ;                        ; - BenchmarkASM::classArrGet@2 (line 81)
│  │                                 ;                        ; - classArrGet_avgt_jmhStub@17 (line 186)
│  │  mov    0x50(%rsp),%r11         ; r11 = this 
│  │  mov    0x10(%r11),%r11d        ; r11d = i               ;*getfield i {reexecute=0 rethrow=0 return_oop=0}
│  │                                 ;                        ; - BenchmarkASM::classArrGet@6 (line 81)
│  │                                 ;                        ; - classArrGet_avgt_jmhStub@17 (line 186)
│  │  mov    0xc(%r12,%r10,8),%r9d   ; r9d = classArr.length  ; implicit exception: dispatches to 0x000000011d0f06d4
│  │  cmp    %r9d,%r11d              ; if (classArr.length <= i) 
│  │  nopw   0x0(%rax,%rax,1)        ; ...                
│  │  jae    0x000000011d0f067b      ; goto 0x11d0f067b
│  │  shl    $0x3,%r10               ; r10 = classArr.length * 8
│  │  mov    0x10(%r10,%r11,4),%r10d ; r10d = classArr[i]     ;*aaload {reexecute=0 rethrow=0 return_oop=0}
│  │                                 ;                        ; - BenchmarkASM::classArrGet@9 (line 81)
│  │                                 ;                        ; - classArrGet_avgt_jmhStub@17 (line 186)
│  │  mov    0xc(%r12,%r10,8),%edx   ; edx = r10.x            ; implicit exception: dispatches to 0x000000011d0f06e4
│  │                                 ;                        ;*getfield x {reexecute=0 rethrow=0 return_oop=0}
│  │                                 ;                        ; - BenchmarkASM::classArrGet@10 (line 81)
│  │                                 ;                        ; - classArrGet_avgt_jmhStub@17 (line 186)
│  │  mov    (%rsp),%rsi             ;       
│  │  data16 xchg %ax,%ax            ;        
│  │  callq  0x0000000115641400      ;                        ; ImmutableOopMap {[64]=Oop [72]=Oop [80]=Oop [0]=Oop }
│  │                                                          ;*invokespecial consumeFull {reexecute=0 rethrow=0 return_oop=0}
                   
```

So the apparent tradeoff here is that using `RecordArray` generates more machine code, but it allows 
utilizing memory more efficiently.

However, it's not guaranteed that JIT will always perform these optimizations,
so it's important to benchmark your code on your own JVM, especially if you decide to use `RecordArray`.

---

## Benchmarks

All benchmarks were run on my MacBook Pro (2.9 GHz Intel Core i7, 16 GB RAM, SSD) with 
JDK 15.0.2, Java HotSpot(TM) 64-Bit Server VM, 15.0.2+7-27.

Abstract from [BenchmarkAccessOneField](../src/jmh/java/com.aivean.testrecarr/BenchmarkAccessOneField.java):
```
Benchmark                                (n)  (rngAccess)  (shuffleCls)  Mode  Cnt        Score         Error  Units
BenchmarkAccessOneField.noopBaseline  100000        false         false  avgt    5   233993.926 ±     688.871  ns/op
BenchmarkAccessOneField.classArrGet   100000        false         false  avgt    5   312322.361 ±   56636.230  ns/op
BenchmarkAccessOneField.intArrGet     100000        false         false  avgt    5   296363.542 ±   20275.233  ns/op
BenchmarkAccessOneField.recArrGet     100000        false         false  avgt    5   333413.362 ±   12529.800  ns/op
```
Unit: ns per 100000 loop iterations, lower is better.

Although very close, when data is accessed sequentially and when object array was initialized in order, RecordArray 
is the slowest.

---

However, the situation changes, when memory is written:
```
Benchmark                                (n)  (rngAccess)  (shuffleCls)  Mode  Cnt        Score         Error  Units
BenchmarkAccessOneField.classArrSet   100000        false         false  avgt    5    78360.832 ±    1034.406  ns/op
BenchmarkAccessOneField.intArrSet     100000        false         false  avgt    5    34023.947 ±    1630.673  ns/op
BenchmarkAccessOneField.recArrSet     100000        false         false  avgt    5    33726.757 ±     233.818  ns/op
```
In the absence of `Blackhole`, JIT was able to unroll loops and 
vectorize sequential memory writes in case of `int[]` and RecordArray,
since both of them use linear memory layout.

---

However, the most advantageous scenario for RecordArray is when class array was shuffled, so the references
to objects lead to different memory locations in the heap:

```
Benchmark                                (n)  (rngAccess)  (shuffleCls)  Mode  Cnt        Score         Error  Units
BenchmarkAccessOneField.noopBaseline  100000        false          true  avgt    5   236628.301 ±    6888.255  ns/op
BenchmarkAccessOneField.classArrGet   100000        false          true  avgt    5   492894.887 ±   35842.178  ns/op
BenchmarkAccessOneField.intArrGet     100000        false          true  avgt    5   304254.954 ±   58351.720  ns/op
BenchmarkAccessOneField.recArrGet     100000        false          true  avgt    5   328531.670 ±    1778.968  ns/op

BenchmarkAccessOneField.classArrSet   100000        false          true  avgt    5   183640.110 ±    8232.544  ns/op
BenchmarkAccessOneField.intArrSet     100000        false          true  avgt    5    34681.582 ±     270.818  ns/op
BenchmarkAccessOneField.recArrSet     100000        false          true  avgt    5    33831.184 ±    1493.991  ns/op
```

---

When all three fields are read and written at once, class array is faster or par with parallel `int[]` and
`RecordArray` (unless memory is shuffled) see [BenchmarkAccessAllFields](../src/jmh/java/com.aivean.testrecarr/BenchmarkAccessAllFields.java):

```
Benchmark                                 (n)  (rngAccess)  (shuffleCls)  Mode  Cnt        Score         Error  Units
 BenchmarkAccessAllFields.noopBaseline  100000        false         false  avgt    5   256589.456 ±   14145.265  ns/op
 BenchmarkAccessAllFields.classArrGet   100000        false         false  avgt    5   417680.819 ±  235336.237  ns/op
 BenchmarkAccessAllFields.intArrGet     100000        false         false  avgt    5   459436.492 ±    9240.779  ns/op
 BenchmarkAccessAllFields.recArrGet     100000        false         false  avgt    5   482345.510 ±   12947.381  ns/op

 BenchmarkAccessAllFields.classArrSet   100000        false         false  avgt    5   206402.825 ±    6026.232  ns/op
 BenchmarkAccessAllFields.intArrSet     100000        false         false  avgt    5   201216.948 ±   14507.019  ns/op
 BenchmarkAccessAllFields.recArrSet     100000        false         false  avgt    5   199301.525 ±    4958.942  ns/op
```

---

[BenchmarkBrownianMotion](../src/jmh/java/com.aivean.testrecarr/BenchmarkBrownianMotion.java) is 
intended to compare performance of RecordArray vs array of objects in the "real world" scenario. 
It's a toy simulator for movement and collisions between 10000 2d points (or, rather, circles).

Here `p2pCols` is whether point to point collisions are enabled (if not, only wall collisions are simulated).

`recImpl==true` corresponds to RecordArray implementation, `recImpl==false` to array of objects.

```
Benchmark                           (n)  (p2pCols)  (recImpl)  Mode  Cnt   Score   Error  Units
BenchmarkBrownianMotion.simulate  10000      false      false  avgt    5   0.053 ± 0.005  ms/op
BenchmarkBrownianMotion.simulate  10000      false       true  avgt    5   0.032 ± 0.001  ms/op
BenchmarkBrownianMotion.simulate  10000       true      false  avgt    5  71.456 ± 4.416  ms/op
BenchmarkBrownianMotion.simulate  10000       true       true  avgt    5  58.539 ± 1.728  ms/op
```

Side note, I tried porting this benchmark to Rust, using mutable Vector of structs, and performance was 
the same as in Java with RecordArray (both benchmarks were run on my laptop).