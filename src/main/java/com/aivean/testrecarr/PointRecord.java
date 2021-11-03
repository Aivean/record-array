package com.aivean.testrecarr;

import com.aivean.recarr.Record;

@Record
public interface PointRecord {

    int getX();

    int getY();

    int getZ();

    void setX(int x);

    void setY(int y);

    void setZ(int z);

    default void setAll(int i) {
        setX(i);
        setY(i);
        setZ(i);
    }

    default int getAll() {
        return getX() ^ getY() ^ getZ();
    }
}
