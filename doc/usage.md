# Usage

1. Use [Jitpack](https://jitpack.io/) to add the project as a dependency.
2. Ensure that annotation processing is enabled for the project.

   Note: currently for simplicity the annotation processor and runtime are bundled in the same module.

3. Create POJO interface, with following conventions:
    
   * getters are named `getXxx` (or `isXxx` for boolean properties),
      and takes no arguments.
   * setters are named `setXxx`, return `void` and accept the same 
      type as the getter as a single argument
   * optionally, add the method `copy()`, that takes no arguments 
      and returns the interface itself
   * any `default` methods are allowed (they are ignored by generator)

4. Annotate the interface with `@com.aivean.recarr.Record` annotation.

For example:
```java
@Record
public interface Person {
    String getName();
    void setName(String name);
    int getAge();
    void setAge(int age);
    Person copy();
    default String prettyPrint() {
        return "Person{name=" + getName() + ", age=" + getAge() + "}";
    }
}
```

Create an instance of the `RecordArray<Person>`:
```java
RecordArray<Person> people = RecordArray.create(Person.class, 10);
Person person = people.get(0);
people.set(1, person);
```

Record Array supports up to three dimensions:
```
RecordArray<Person> people = RecordArray.create(Person.class, 10, 20, 30);
Person person = people.get(5, 12, 23);
people.set(9, 19, 29, person);
```

Note: the API is designed in such way that it doesn't 
require additional support from IDE. Even when annotation processing is disabled,
the code that create and uses the RecordArray is valid and compiles. 
However, if annotation processing is not enabled, the runtime exception 
will be thrown when the code is executed.

See [RecordArray javadocs](../record-array/src/main/java/com/aivean/recarr/RecordArray.java)  for API details, see also [internals](internals.md) and [quirks](quirks.md).
