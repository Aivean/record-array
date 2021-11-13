# TODO

This is a list of possible improvements
and future work in no particular order.

1. Make `RecordArray` extend java `Collection` or `List` interface
2. Add more benchmarks, specifically for the cases when `RecordArray` loses to 
   alternatives, and for multidimensional arrays
3. Add more tests
4. Growing/resizable variant of `RecordArray`?
5. Add more utility methods to `RecordArray`, such as swap, reverse, search, range copy, sort, etc. 
6. Split annotation processing and runtime into separate libraries, hide
      annotation processor (lombok-style) from the user
7. Publish to maven central
8. Create pure Kotlin version using [KSP](https://kotlinlang.org/docs/ksp-overview.html) 