package com.aivean.testrecarr;

import com.aivean.recarr.Record;

import java.util.List;

@Record
public interface ComplexRecord {

    ComplexRecord copy();

    int getIntField();
    boolean getBoolField();
    String getStringField();
    char getCharField();
    byte getByteField();
    short getShortField();
    long getLongField();
    float getFloatField();
    double getDoubleField();
    Object getObjectField();
    Object[] getObjectArrayField();

    Boolean getBooleanWrapperField();
    Byte getByteWrapperField();
    Character getCharacterWrapperField();
    Short getShortWrapperField();
    Integer getIntegerWrapperField();
    Long getLongWrapperField();
    Float getFloatWrapperField();
    Double getDoubleWrapperField();

    void setIntField(int value);
    void setBoolField(boolean value);
    void setStringField(String value);
    void setCharField(char value);
    void setByteField(byte value);
    void setShortField(short value);

    List<int []>[][][] getListArrayField();
    void setListArrayField(List<int []>[][][] value);
}
