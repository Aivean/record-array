# Internals

The library consists of the following four parts:

* [`@Record` annotation](../record-array/src/main/java/com/aivean/recarr/Record.java)
* [`RecordArray` interface](../record-array/src/main/java/com/aivean/recarr/RecordArray.java)
* internal [`RecordArrayFactory` class](../record-array/src/main/java/com/aivean/recarr/RecordArrayFactory.java)
* [`RecordAnnotationProcessor`](../record-array/src/main/java/com/aivean/recarr/RecordAnnotationProcessor.java)

`@Record` used to mark interfaces to be used as array element,
`RecordArray` serves as a factory for arrays and the interface to 
represent them.

`RecordArrayFactory` links the `RecordArray.create` method 
with the actual generated implementation.

---

How the magic happens?

For each interface marked with `@Record` annotation,
the `RecordAnnotationProcessor` generates a class implementing
the interface and the `RecordArray` interface.

Suppose you have [the following interface](../src/main/java/com/aivean/testrecarr/SimpleRecord.java):
```java
@Record
public interface SimpleRecord {
    String getName();
    void setName(String name);

    int getAge();
    void setAge(int age);

    boolean isMale();
    void setMale(boolean value);

    SimpleRecord copy();

    default String getNameAndAge() {
        return getName() + " " + getAge();
    }
}
```

The `RecordAnnotationProcessor` generates the class 
`com.aivean.recarr.RecordArrayFactoryImpl`, which 
will have the following nested class (some parts are omitted for brevity):

```java
static class SimpleRecordImpl$1$9500 implements RecordArray<SimpleRecord>{
    final int __dim0;
    final int __dim1;
    final int __dim2;

    // generated fields
    private java.lang.String[] Name;
    private int[] Age;
    private boolean[] Male;

    // constructor
    SimpleRecordImpl$1$9500(int... dimensions) {
        // some checks omitted for brevity
        __dim0 = dimensions[0];
        __dim1 = dimensions.length > 1 ? dimensions[1] : 1;
        __dim2 = dimensions.length > 2 ? dimensions[2] : 1;
        
        int __l = __dim0 * __dim1 * __dim2;

        // initialize generated fields
        Name = new java.lang.String[__l];
        Age = new int[__l];
        Male = new boolean[__l];
    }

    // methods
    public int size() {
        return __dim0 * __dim1 * __dim2;
    }

    public $$Record get(int i) {
        return new $$Record(i);
    }
    public $$Record get(int i0, int i1) {
        return new $$Record(i0 * __dim1 + i1);
    }
    // ...
    public void set(int i, com.aivean.testrecarr.SimpleRecord value) {
        this.Name[i] = value.getName();
        this.Age[i] = value.getAge();
        this.Male[i] = value.isMale();
    }

    final class $$Record implements com.aivean.testrecarr.SimpleRecord {
        private final int __index;
    
        $$Record(int index) {
            this.__index = index;
        }
    
        public java.lang.String getName() { return Name[__index]; }
        
        public void setName(java.lang.String value) {
            SimpleRecordImpl$1$9500.this.Name[__index] = value;
        }
        
        public int getAge() { return Age[__index]; }
        
        // ...
        
        public com.aivean.testrecarr.SimpleRecord copy() {
            return new $$DetachedRecord(this);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof com.aivean.testrecarr.SimpleRecord)) return false;
            com.aivean.testrecarr.SimpleRecord that = (com.aivean.testrecarr.SimpleRecord) o;
            if (!Objects.equals(this.getName(), that.getName())) return false;
            if (this.getAge() != that.getAge()) return false;
            if (this.isMale() != that.isMale()) return false;
            return true;
        }
        
        @Override
        public int hashCode() {
            int result;
            result = (Objects.hashCode(getName()));
            result =  result * 31 + (getAge());
            result =  result * 31 + (isMale() ? 1 : 0);
            return result;
        }
    }

    final class $$DetachedRecord implements com.aivean.testrecarr.SimpleRecord {
        private java.lang.String Name;
        private int Age;
        private boolean Male;
    
        // Constructor
        $$DetachedRecord(com.aivean.testrecarr.SimpleRecord other) {
            this.Name = other.getName();
            this.Age = other.getAge();
            this.Male = other.isMale();
        }
    
        public java.lang.String getName() { return Name; }
        
        public void setName(java.lang.String value) {
            this.Name = value;
        }
        
        public int getAge() { return Age; }
        // ...
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof com.aivean.testrecarr.SimpleRecord)) return false;
            com.aivean.testrecarr.SimpleRecord that = (com.aivean.testrecarr.SimpleRecord) o;
            if (!Objects.equals(this.getName(), that.getName())) return false;
            if (this.getAge() != that.getAge()) return false;
            if (this.isMale() != that.isMale()) return false;
            return true;
        }
        
        @Override
        public int hashCode() {
            // ...
        }
    }
}
```

As you can see, there are two generated implementations 
of the `SimpleRecord` interface. 

First is the `$$Record` class, which is just the pointer into 
the array of records (it has two fields, reference to the array and index).

Second is the `$$DetachedRecord` class, which is a copy of the record,
independent of the array. The `$$Record.copy()` method creates an instance of this class.

Another thing to note is that `equals` method for both classes 
accepts any instance of `SimpleRecord` interface. This allows to 
compare `$$Record` to `$$DetachedRecord` and vice versa, but 
may lead to unexpected results if some user class that 
implements the `SimpleRecord` doesn't follow this convention.