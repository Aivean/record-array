package com.aivean.testrecarr;

import com.aivean.recarr.RecordArray;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class RecArrTest {

    @Test
    public void testGetSet() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 10);
        arr.get(0).setName("a");
        arr.get(3).setName("b");

        Assert.assertEquals(arr.get(0).getName(), "a");
        Assert.assertEquals(arr.get(3).getName(), "b");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testWeirdType() {
        RecordArray<ComplexRecord> arr = RecordArray.create(ComplexRecord.class, 10);

        // listArrayField type is List<int[]>[][][] value

        List[][][] lst = new ArrayList[][][]{{{new ArrayList<int[]>()}}};
        arr.get(0).setListArrayField(lst);
        arr.get(0).getListArrayField()[0][0][0].add(new int[]{1, 2, 3});
        Assert.assertEquals(arr.get(0).getListArrayField()[0][0][0].get(0)[2], 3);
    }

    @Test
    public void testCopyEqualsAndHashCode() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 10);

        Assert.assertEquals(arr.get(0).getAge(), 0);

        arr.get(0).setAge(1);
        arr.get(0).setName("a");
        arr.get(0).setMale(false);

        SimpleRecord copy = arr.get(0).copy();
        Assert.assertEquals(copy.getAge(), 1);
        Assert.assertEquals(copy.getName(), "a");
        Assert.assertFalse(copy.isMale());

        Assert.assertEquals(arr.get(0), copy);
        Assert.assertEquals(arr.get(0).hashCode(), copy.hashCode());

        copy.setAge(2);
        Assert.assertEquals(copy.getAge(), 2);
        Assert.assertEquals(arr.get(0).getAge(), 1);

        Assert.assertNotEquals(arr.get(0), copy);
        Assert.assertNotEquals(arr.get(0).hashCode(), copy.hashCode());
    }

    @Test
    public void testAssign() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 10);

        Assert.assertEquals(arr.get(0).getAge(), 0);

        arr.get(0).setAge(1);
        arr.get(0).setName("a");
        arr.get(0).setMale(false);

        Assert.assertEquals(arr.get(1).getAge(), 0);
        Assert.assertNotEquals(arr.get(0), arr.get(1));

        arr.set(1, arr.get(0));
        Assert.assertEquals(arr.get(1).getAge(), 1);
        Assert.assertEquals(arr.get(0), arr.get(1));
    }

    @Test
    public void testDefaultMethods() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 10);
        arr.get(0).setAge(1);
        arr.get(0).setName("a");

        Assert.assertEquals(arr.get(0).getNameAndAge(), "a 1");
    }

}
