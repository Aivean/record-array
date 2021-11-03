package com.aivean.recarr;

/**
 * RecordArray is a multidimensional array of "records" (up to 3 dimensions).
 * It represents the SoA (Struct of Arrays) data structure.
 */
public interface RecordArray<T> {

    /**
     * Return the "proxy" to the record at the given index (1d).
     */
    T get(int index);

    /**
     * Return the "proxy" to the record at the given index (2d).
     * This is equivalent of get(i * dimensions[1] + j),
     * where `dimensions` is the dimensions array used passed to RecordArray.create().
     */
    T get(int i, int j);

    /**
     * Return the "proxy" to the record at the given index (3d).
     * This is equivalent of get( (i * dimensions[1] + j) * dimensions[2] + k),
     * where `dimensions` is the dimensions array used passed to RecordArray.create().
     */
    T get(int i, int j, int k);

    /**
     * Sets the fields of the record at the given index (1d).
     * Note: as RecordArray is doesn't hold the actual objects, the identity of `value` is not preserved,
     * only the values of its fields, retrieved via getters.
     */
    void set(int index, T value);

    /**
     * Sets the fields of the record at the given index (2d).
     * See {@link #get(int, int)} for details of how index is actually calculated.
     * See {@link #set(int, Object)} for details of how the identity of `value` is not preserved.
     */
    void set(int i, int j, T value);

    /**
     * Sets the fields of the record at the given index (3d).
     * See {@link #get(int, int)} for details of how index is actually calculated.
     * See {@link #set(int, Object)} for details of how the identity of `value` is not preserved.
     */
    void set(int i, int j, int k, T value);

    /**
     * Returns the number of records in the array.
     * This is equivalent of the product of the dimensions.
     */
    int size();

    /**
     * Create a new RecordArray with given dimensions (size is the product of dimensions).
     * If less than 3 dimensions are given, the remaining dimensions are filled with 1.
     * <p>
     * If annotation processing is not set up correctly of parameters fail the validation,
     * an IllegalArgumentException is thrown.
     *
     * @param recordClass Class of the records. Must be an interface marked with @Record.
     *                    If not, an IllegalArgumentException exception is thrown.
     * @param dimensions  dimensions of the array, must be >= 1.
     * @param <T>         type of the records
     * @return new RecordArray with given dimensions
     */
    static <T> RecordArray<T> create(Class<T> recordClass, int... dimensions) {
        RecordArray<T> res = RecordArrayFactory.create(recordClass, dimensions);
        if (res == null) {
            throw new IllegalArgumentException("Unsupported record class: " + recordClass +
                    "\nIs annotation processing enabled? Perhaps, project rebuild is needed.");
        }
        return res;
    }
}