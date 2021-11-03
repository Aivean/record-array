package com.aivean.testrecarr;

import com.aivean.recarr.RecordArray;
import org.testng.Assert;
import org.testng.annotations.Test;

public class QuirksTest {
    /**
      Test that the RecordArray.get() method always returns new object,
      meaning that identity of the object is not preserved.
     */
    @Test
    public void testIdentityAbsence() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 10);
        arr.get(0).setAge(1);
        arr.get(0).setName("a");
        arr.get(0).setMale(true);

        // repeated get(0) returns new object, which are equal, but not identical
        Assert.assertEquals(arr.get(0), arr.get(0));
        //noinspection SimplifiableAssertion (IntelliJ suggests replacing this with `assertNotSame`, which is wrong)
        Assert.assertFalse(arr.get(0) == arr.get(0));
    }

    /**
     * RecordArray.get() returns a "handle" or "cursor" to the index in the array.
     * Think of it as a C pointer to the position in the array. It's just a glorified array index.
     * It's different from the traditional Java reference to the object.
     * When you overwrite the value of element of the RecordArray, all stored handles will reflect the update.
     * It's similar to how array of primitives (e.g. int[]) works, and different from how
     * array of references (e.g. Object[]) works.
     */
    @Test
    public void testHandlesToTheElements() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 10);
        arr.get(0).setAge(1);
        arr.get(0).setName("a");
        arr.get(0).setMale(true);

        arr.get(1).setAge(2);
        arr.get(1).setName("b");
        arr.get(1).setMale(false);

        // Let's try the "swap" idiom
        SimpleRecord tmp = arr.get(0);
        arr.set(0, arr.get(1));
        arr.set(1, tmp);

        // If `tmp` was a "traditional" java reference, arr[0] and arr[1] would be swapped
        // however, that is not the case. When arr.set(0, arr.get(1)) is called,
        // arr[0] is modified, and `tmp` reflects this change.
        // So, arr[0], arr[1] and `tmp` will have the same value.
        Assert.assertEquals(arr.get(0), arr.get(1));
        Assert.assertEquals(arr.get(1), tmp);

        // To address this, we can use the `copy()` method, that creates a "detached" copy of the array element.
        // Note, the `copy()` method has to actually copy all the fields, so it's not a cheap operation.
        // That is one of the drawbacks of RecordArray, compared to array of references.
        arr.get(0).setAge(1);
        arr.get(0).setName("a");
        arr.get(0).setMale(true);

        arr.get(1).setAge(2);
        arr.get(1).setName("b");
        arr.get(1).setMale(false);

        tmp = arr.get(0).copy();
        arr.set(0, arr.get(1));
        arr.set(1, tmp);

        // RecordArray elements were swapped correctly
        Assert.assertNotEquals(arr.get(0), arr.get(1));
        Assert.assertEquals(arr.get(0).getAge(), 2);
        Assert.assertEquals(arr.get(1).getAge(), 1);
    }

    /**
     * RecordArray stores "records", which are more like C structs than traditional Java objects.
     * Thus, the polymorphism is lost. For example, it's allowed to implement the interface, used to
     * access the RecordArray elements, and use it to set RecordArray elements, or check equality.
     * However, it's important to remember, that RecordArray will always copy the fields of the elements,
     * and store them in the "parallel array" form. So, the identity and the type of the elements are not preserved.
     */
    @Test
    public void testPolymorphism() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 10);
        MyRecord rec = new MyRecord();
        rec.age = 1;
        rec.name = "a";
        rec.setMale(true);

        arr.set(0, rec);
        // RecordArray element was updated, as expected
        Assert.assertEquals(arr.get(0).getAge(), 1);
        Assert.assertEquals(arr.get(0).getName(), "a");
        Assert.assertTrue(arr.get(0).isMale());

        // Equals may not be symmetrical when custom Record implementation is involved
        //noinspection SimplifiableAssertion
        Assert.assertTrue(arr.get(0).equals(rec));

        // `MyRecord` doesn't implement `equals()` method
        //noinspection SimplifiableAssertion
        Assert.assertFalse(rec.equals(arr.get(0)));

        //noinspection ConstantConditions
        Assert.assertTrue(arr.get(0) instanceof SimpleRecord);

        //Returned class is internal implementation of SimpleRecord, not the class
        // we used to set the RecordArray element
        Assert.assertFalse(arr.get(0) instanceof MyRecord);
    }

    static class MyRecord implements SimpleRecord {
        private int age;
        private String name;
        private boolean male;

        @Override
        public int getAge() {
            return age;
        }

        @Override
        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean isMale() {
            return male;
        }

        @Override
        public void setMale(boolean male) {
            this.male = male;
        }

        @Override
        public SimpleRecord copy() {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

}
