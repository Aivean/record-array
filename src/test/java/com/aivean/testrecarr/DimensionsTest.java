package com.aivean.testrecarr;

import com.aivean.recarr.RecordArray;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DimensionsTest {

    @Test
    public void test1Dim() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 10);

        Assert.assertEquals(arr.size(), 10);

        for (int i = 0; i < 10; i++) {
            arr.get(i).setAge(i);
        }
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(arr.get(i).getAge(), i);
        }
    }

    @Test
    public void test2Dim() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 20, 10);

        Assert.assertEquals(arr.size(), 20 * 10);

        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 10; j++) {
                arr.get(i, j).setAge(i);
                arr.get(i, j).setName("name" + j);
            }
        }
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 10; j++) {
                Assert.assertEquals(arr.get(i, j).getAge(), i);
                Assert.assertEquals(arr.get(i, j).getName(), "name" + j);
            }
        }
    }

    @Test
    public void test3Dim() {
        RecordArray<SimpleRecord> arr = RecordArray.create(SimpleRecord.class, 20, 10, 5);

        Assert.assertEquals(arr.size(), 20 * 10 * 5);

        int index = 0;
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 10; j++) {
                for (int k = 0; k < 5; k++) {
                    arr.get(i, j, k).setAge(index);
                    arr.get(i, j, k).setName("" + i + ":" + j + ":" + k);
                    index++;
                }
            }
        }

        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 10; j++) {
                for (int k = 0; k < 5; k++) {
                    Assert.assertEquals(arr.get(i, j, k).getName(), "" + i + ":" + j + ":" + k);
                }
            }
        }

        for (int i = 0; i < arr.size(); i++) {
            Assert.assertEquals(arr.get(i).getAge(), i);
        }
    }

}
