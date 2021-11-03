# Quirks

Although RecordArray tries to be as unobtrusive as possible,
it's not a first-class citizen. There are some inevitable tradeoffs:

* Loss of identity of records
* "Record" behaves differently compared to a plain java object (it's essentially a pointer into the array)
* Polymorphism is not possible, all records must have the same type
* [Performance](performance.md) is highly dependent on method inlining and object creation elimination

See comments and examples in [QuirksTest.java](../src/test/java/com/aivean/testrecarr/QuirksTest.java).
