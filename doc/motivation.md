# Motivation

The sole purpuse of this project is to increase performance 
in some very specific cases. If you are not concerned with 
memory compactness and layout, CPU cache locality, 
GC pressure and pauses, you can ignore this project.

---

Currently, as of version 16, Java still lacks the language
support for arrays or collections of "records" (structures). 
Value types of  [project Valhalla](https://openjdk.java.net/jeps/169)
aim to solve this issue, but they are still long in the works.

---

Array of "records" correspond to the following C++ code:
```cpp
struct { int x; int y; } points [10];
```

The idiomatic way to model this in Java would be the array of 
objects:
```java
class Point {
    int x;
    int y;
};
Point[] points = new Point[10];
```

However, the important difference is that in C++ the 
memory layout of the array is following:

```
[arr₀x][arr₀y][arr₁x][arr₁y][arr₂x][arr₂y]...
```
All elements of the array are located compactly together.

While `Point[]` in Java is an array of references to objects:
```
[arr₀ ref][arr₁ ref][arr₂ ref] ...
   │            \            \________________________          
   │             \___________                         \
   │                         \                         \
   ↓                          ↘                         ↘
 [header][arr₀x][arr₀y] ... [header][arr₁x][arr₁y] ... [header][arr₂x][arr₂y]  
```

There are three problems with this layout:
1. Wasted memory:

   Extra memory required to store references and object headers.

2. Poor [spacial locality](https://en.wikipedia.org/wiki/Locality_of_reference):
      
   When such array is processed sequentially, it will generate a lot of CPU cache misses.
   The situation is even worse, if objects were created in different order or 
   in different time, and they are physically spread out on the heap.

4. Increased GC pressure: 

   GC has to visit every element of the array every mark-sweep cycle.


---

To mitigate these problems, Java programmers often resort to 
the approach called [parallel arrays](https://en.wikipedia.org/wiki/Parallel_array)
or [structure of arrays](https://en.wikipedia.org/wiki/AoS_and_SoA):
```java
var pointsX = new int[10];
var pointsY = new int[10];
pointsX[0] = 1;
pointsY[0] = 2;
```
This approach mitigates most of the problems of the previous approach,
but has its own disadvantages:

1. It obscures the relationship between fields of a single record:
    
    Bundle of arrays is no longer a single object. 
    It's not possible to have class methods.
   
2. Since the bundle of fields is not a "thing", passing it around it tedious and error-prone.
3. Greatly raises the possibility of errors:
 
   Any insertion, deletion, or move must always be applied consistently to all of the arrays, 
   or the arrays will no longer be synchronized with each other, leading to bizarre outcomes.

5. [And more](https://en.wikipedia.org/wiki/Parallel_array#Pros_and_cons).

---

This library is intended to solve the first three problems. It provides a
simple and convenient interface-based API, 
similar to the array of objects, while using a compact memory layout of the 
second approach (parallel arrays) internally,
often leading to a [performance increase](performance.md).

See [usage](usage.md) and [internals](internals.md) for more details.