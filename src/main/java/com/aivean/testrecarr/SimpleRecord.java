package com.aivean.testrecarr;

import com.aivean.recarr.Record;

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
