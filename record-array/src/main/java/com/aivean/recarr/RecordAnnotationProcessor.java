package com.aivean.recarr;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RecordAnnotationProcessor extends AbstractProcessor {

    private Filer filer;

    private static final Set<String> primitiveTypes = new HashSet<>(Arrays.asList(
            "boolean", "byte", "char", "double", "float", "int", "long", "short"
    ));

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        filer = processingEnv.getFiler();
    }

    /**
     * Pads each line of the string with the specified number of spaces.
     */
    private static String pad(String s, int length) {
        String padding = String.join("", Collections.nCopies(length, " "));
        return Arrays.stream(s.split("\n")).map(line -> padding + line).collect(Collectors.joining("\n"));
    }

    private static String pad(String s) {
        return pad(s, 4);
    }

    static String generateHashCode(Map<String, String> fieldsToTypes, Map<String,
            String> fieldsToGetters) {

        StringBuilder sb = new StringBuilder();
        sb.append("@Override\n" +
                  "public int hashCode() {\n" +
                  "    int result;\n");
        if (fieldsToTypes.containsValue("double")) {
            sb.append("    long temp;");
        }
        boolean first = true;
        for (Map.Entry<String, String> entry : fieldsToGetters.entrySet()) {
            String fieldName = entry.getKey();
            String getterName = entry.getValue();
            String fieldType = fieldsToTypes.get(fieldName);

            if (fieldType.equals("double")) {
                sb.append("    temp = Double.doubleToLongBits(").append(getterName).append("());\n");
            }
            sb.append("    result = ");
            if (!first) {
                sb.append(" result * 31 + ");
            } else {
                first = false;
            }
            sb.append("(");
            if (primitiveTypes.contains(fieldType)) {
                // (floatField != +0.0f ? Float.floatToIntBits(floatField) : 0)
                // byte, short, int, char -> cast to int
                if ("double".equals(fieldType)) {
                    sb.append("(int) (temp ^ (temp >>> 32))");
                } else if ("float".equals(fieldType)) {
                    sb.append("(int) ((").append(getterName).append("() != +0.0f ? Float.floatToIntBits(")
                            .append(getterName).append("()) : 0))");
                } else if ("long".equals(fieldType)) {
                    sb.append("(int) (").append(getterName).append("() ^ (")
                            .append(getterName).append("() >>> 32))");
                } else if ("boolean".equals(fieldType)) {
                    sb.append(getterName).append("() ? 1 : 0");
                } else {
                    sb.append(getterName).append("()");
                }
            } else {
                // use Objects.hashCode()
                sb.append("Objects.hashCode(").append(getterName).append("())");
            }
            sb.append(");\n");
        }

        sb.append("    return result;\n");
        sb.append("}");
        return sb.toString();
    }

    static String generateEquals(String className, Map<String, String> fieldsToTypes, Map<String,
            String> fieldsToGetters) {

        String compareFieldsStr = fieldsToGetters.entrySet().stream().map(e -> {
            String fieldName = e.getKey();
            String getterName = e.getValue();
            String type = fieldsToTypes.get(fieldName);

            String ret;
            if (type.equals("double")) {
                ret = "if (Double.compare(this.${getter_name}(), that.${getter_name}()) != 0) return false;";
            } else if (type.equals("float")) {
                ret = "if (Float.compare(this.${getter_name}(), that.${getter_name}()) != 0) return false;";
            } else if (primitiveTypes.contains(type)) {
                ret = "if (this.${getter_name}() != that.${getter_name}()) return false;";
            } else {
                ret = "if (!Objects.equals(this.${getter_name}(), that.${getter_name}())) return false;";
            }
            return ret.replace("${getter_name}", getterName);
        }).collect(Collectors.joining("\n"));

        return ("@Override\n" +
                "public boolean equals(Object o) {\n" +
                "    if (this == o) return true;\n" +
                "    if (!(o instanceof ${record_type})) return false;\n" +
                "    ${record_type} that = (${record_type}) o;\n" +
                "${compare_fields}\n" +
                "    return true;\n" +
                "}\n")
                .replace("${record_type}", className)
                .replace("${compare_fields}", pad(compareFieldsStr));
    }

    static String generateFieldInitializer(String type, String fieldName) {
        if (type.endsWith("[]")) {
            String newType = type;
            if (type.contains("<")) {
                // remove everything between first < and last >
                newType = type.substring(0, type.indexOf("<")) + type.substring(type.lastIndexOf(">") + 1);
            }
            // insert [__l] before the first [
            newType = newType.replaceFirst("\\[", "[__l][");
            return fieldName + " = (" + type + "[])(new " + newType + ");";
        } else {
            return fieldName + " = new " + type + "[__l];";
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<Element> els = new ArrayList<>();

        for (Element el : roundEnv.getElementsAnnotatedWith(Record.class)) {
            if (el.getKind() != ElementKind.INTERFACE) {
                String msg = "Only interfaces can be annotated with @Record";
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, el);
                throw new IllegalStateException(msg + el);
            }
            els.add(el);
        }

        // Generated code, implementing each RecordArray<T> for each interface T, marked with @Record
        List<String> internalClassesImpls = new ArrayList<>(els.size());

        // map of simple type name -> mangled type name (since simple type names are not unique)
        Map<String, String> simpleNamesToMangled = new HashMap<>();
        for (Element el : els) {
            String className = el.getSimpleName().toString();
            String mangledName = className + "Impl$" + simpleNamesToMangled.size() + "$" + (int) (Math.random() * 10000);
            simpleNamesToMangled.put(className, mangledName);
        }

        for (Element el : els) {
            internalClassesImpls.add(getInternalClassImplStr(el, simpleNamesToMangled));
        }

        final String factoryInitStr =
                els.stream().map(el ->
                        "factories.put(" +
                        el.asType().toString() + ".class, " +
                        "RecordArrayFactoryImpl." + simpleNamesToMangled.get(el.getSimpleName().toString()) +
                        ".class.getDeclaredConstructor(int [].class));"
                ).collect(Collectors.joining("\n"));


        final String recordArrayFactoryStr = (
                "package com.aivean.recarr;\n" +
                "\n" +
                "import java.lang.reflect.Constructor;\n" +
                "import java.lang.reflect.InvocationTargetException;\n" +
                "import java.util.HashMap;\n" +
                "import java.util.Map;\n" +
                "import com.aivean.recarr.RecordArray;\n" +
                "import java.util.Objects;\n" +
                "\n" +
                "class RecordArrayFactoryImpl {\n" +
                "    private static final Map<Class, Constructor> factories = new HashMap<>();\n" +
                "\n" +
                "    static {\n" +
                "        try {\n" +
                "${factory_init}\n" +
                "        } catch (Exception e) {\n" +
                "            throw new RuntimeException(\"Failed to find constructor\", e);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public static Object create(Class clazz, int... dimensions) {\n" +
                "        Constructor c = factories.get(clazz);\n" +
                "        if (c == null) {\n" +
                "            return null;\n" +
                "        }\n" +
                "        try {\n" +
                "            return c.newInstance(dimensions);\n" +
                "        } catch (Exception e) {\n" +
                "            throw new IllegalStateException(\"Failed to create instance record for \" + clazz, e);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "${internal_classes}\n" +
                "\n" +
                "}\n")
                .replace("${factory_init}", pad(factoryInitStr, 12))
                .replace("${internal_classes}", pad(String.join("\n", internalClassesImpls)));

        try {
            JavaFileObject sourceFile = filer.createSourceFile("com.aivean.recarr.RecordArrayFactoryImpl");
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println(recordArrayFactoryStr);
            }
        } catch (IOException e) {
            // ignore
        }

        return true;
    }

    private String getInternalClassImplStr(Element el, Map<String, String> classNamesToMangled) {
        // detected fields (name -> type)
        LinkedHashMap<String, String> fieldsToTypes = new LinkedHashMap<>();

        // field name -> getter name
        LinkedHashMap<String, String> fieldsToGetters = new LinkedHashMap<>();

        // generated methods for {Type}Imp.$$Record
        List<String> arrRecordMethods = new ArrayList<>();

        // generated methods for {Type}Imp.$$DetachedRecord
        List<String> detachedRecordMethods = new ArrayList<>();

        boolean hasCopyMethod = false;

        String mangledClassName = classNamesToMangled.get(el.getSimpleName().toString());

        for (Element m : el.getEnclosedElements()) {
            if (m.getKind() == ElementKind.METHOD && !m.getModifiers().contains(Modifier.DEFAULT)) {
                ExecutableElement me = (ExecutableElement) m;
                final String name = m.getSimpleName().toString();

                BiConsumer<String, String> validateField = (String fname, String type) -> {
                    if (fname.length() == 0) {
                        throw new IllegalStateException("Invalid getter name: " + el.getSimpleName() + "." + name);
                    }
                    if (fieldsToTypes.containsKey(fname) && !fieldsToTypes.get(fname).equals(type)) {
                        throw new IllegalStateException("Conflicting getter types: " + el.getSimpleName() + "." + name +
                                                        " " + fieldsToTypes.get(fname) + " " + type);
                    }
                };

                Consumer<String> createGetter = (String fname) -> {
                    String type = me.getReturnType().toString();
                    validateField.accept(fname, type);
                    fieldsToTypes.put(fname, type);
                    fieldsToGetters.put(fname, name);
                    arrRecordMethods.add(
                            "public ${type} ${getter_name}() { return ${field_name}[__index]; }\n"
                                    .replace("${type}", me.getReturnType().toString())
                                    .replace("${getter_name}", name)
                                    .replace("${field_name}", fname)
                    );
                    detachedRecordMethods.add("public ${type} ${getter_name}() { return ${field_name}; }\n"
                            .replace("${type}", me.getReturnType().toString())
                            .replace("${getter_name}", name)
                            .replace("${field_name}", fname)
                    );
                };

                if (name.startsWith("get")) {
                    createGetter.accept(name.substring(3));
                } else if (name.startsWith("is")) {
                    createGetter.accept(name.substring(2));
                } else if (name.startsWith("set")) {
                    String fname = name.substring(3);
                    if (me.getParameters().size() != 1) {
                        throw new IllegalStateException("Invalid setter " + el.getSimpleName() + "." + name +
                                                        "; Expected a single parameter");
                    }
                    if (me.getReturnType().getKind() != TypeKind.VOID) {
                        throw new IllegalStateException("Invalid setter " + el.getSimpleName() + "." + name +
                                                        "; Expected a void return type");
                    }
                    String type = me.getParameters().get(0).asType().toString();
                    validateField.accept(fname, type);
                    fieldsToTypes.put(fname, me.getParameters().get(0).asType().toString());
                    arrRecordMethods.add(("public void ${setter_name}(${type} value) {\n" +
                                          "    ${impl_type}.this.${field_name}[__index] = value;\n" +
                                          "}\n")
                            .replace("${setter_name}", name)
                            .replace("${type}", type)
                            .replace("${field_name}", fname)
                            .replace("${impl_type}", mangledClassName)
                    );
                    detachedRecordMethods.add(("public void ${setter_name}(${type} value) {\n" +
                                               "    this.${field_name} = value;\n" +
                                               "}\n")
                            .replace("${setter_name}", name)
                            .replace("${type}", type)
                            .replace("${field_name}", fname)
                    );
                } else if (name.equals("copy")) {
                    if (me.getParameters().size() != 0) {
                        throw new IllegalStateException("Invalid copy method " + el.getSimpleName() + "." + name +
                                                        "; Expected no parameters");
                    }
                    if (!me.getReturnType().equals(el.asType())) {
                        throw new IllegalStateException("Invalid copy method " + el.getSimpleName() + "." + name +
                                                        "; Expected a return type of " + el.asType());
                    }
                    hasCopyMethod = true;
                    arrRecordMethods.add(("public ${type} copy() {\n" +
                                          "    return new $$DetachedRecord(this);\n" +
                                          "}\n")
                            .replace("${type}", el.asType().toString())
                    );
                    detachedRecordMethods.add(("public ${type} copy() {\n" +
                                               "    return new $$DetachedRecord(this);\n" +
                                               "}\n")
                            .replace("${type}", el.asType().toString())
                    );
                }
            }
        }

        {
            String equalsStr = generateEquals(el.asType().toString(), fieldsToTypes, fieldsToGetters);
            String hashCodeStr = generateHashCode(fieldsToTypes, fieldsToGetters);

            arrRecordMethods.add(equalsStr);
            arrRecordMethods.add(hashCodeStr);
            detachedRecordMethods.add(equalsStr);
            detachedRecordMethods.add(hashCodeStr);
        }

        // $$Record implementation
        final String recordImplStr = (
                "final class $$Record implements ${record_type} {\n" +
                "    private final int __index;\n" +
                "\n" +
                "    $$Record(int index) {\n" +
                "        this.__index = index;\n" +
                "    }\n" +
                "\n" +
                "${methods}\n" +
                "}\n")
                .replace("${record_type}", el.asType().toString())
                .replace("${methods}", pad(String.join("\n", arrRecordMethods)));

        final String detachedRecordImplFieldsStr = fieldsToTypes.entrySet().stream().map(e ->
                "private " + e.getValue() + " " + e.getKey() + ";"
        ).collect(Collectors.joining("\n"));

        final String detachedRecordFieldAssignmentsStr = fieldsToGetters.entrySet().stream().map(e ->
                "this." + e.getKey() + " = other." + e.getValue() + "();"
        ).collect(Collectors.joining("\n"));

        // Detached $$DetachedRecord implementation
        final String detachedRecordImplStr = !hasCopyMethod ? "" : ((
                "final class $$DetachedRecord implements ${record_type} {\n" +
                "${detached_record_fields}\n" +
                "\n" +
                "    // Constructor\n" +
                "    $$DetachedRecord(${record_type} other) {\n" +
                "${detached_record_fields_assignments}\n" +
                "    }\n" +
                "\n" +
                "${detached_record_methods}\n" +
                "}\n")
                .replace("${record_type}", el.asType().toString())
                .replace("${detached_record_fields}", pad(detachedRecordImplFieldsStr))
                .replace("${detached_record_fields_assignments}", pad(detachedRecordFieldAssignmentsStr, 8))
                .replace("${detached_record_methods}", pad(String.join("\n", detachedRecordMethods))));

        final String arrayImplFieldsStr = fieldsToTypes.entrySet().stream().map(e ->
                "private final " + e.getValue() + "[] " + e.getKey() + ";"
        ).collect(Collectors.joining("\n"));

        final String arrayImplFieldsInitStr = fieldsToTypes.entrySet().stream().map(e ->
                generateFieldInitializer(e.getValue(), e.getKey())
        ).collect(Collectors.joining("\n"));

        final String arrayImplSetter0 =
                "public void set(int i, " + el.asType().toString() + " value) {\n" +
                pad(fieldsToGetters.entrySet().stream()
                        .map(e -> "this." + e.getKey() + "[i] = value." + e.getValue() + "();")
                        .collect(Collectors.joining("\n"))) +
                "\n}";

        return ("static class ${arr_impl_name} implements RecordArray<${record_type}>{\n" +
                "    final int __dim0;\n" +
                "    final int __dim1;\n" +
                "    final int __dim2;\n" +
                "\n" +
                "    // generated fields\n" +
                "${fields}\n" +
                "\n" +
                "    // constructor\n" +
                "    ${arr_impl_name}(int... dimensions) {\n" +
                "        if (dimensions.length == 0) {\n" +
                "            throw new IllegalArgumentException(\"At least one dimension is required.\");\n" +
                "        }\n" +
                "        if (dimensions.length > 3) {\n" +
                "            throw new IllegalArgumentException(\"Only up to three dimensions are supported.\");\n" +
                "        }\n" +
                "        __dim0 = dimensions[0];\n" +
                "        __dim1 = dimensions.length > 1 ? dimensions[1] : 1;\n" +
                "        __dim2 = dimensions.length > 2 ? dimensions[2] : 1;\n" +
                "\n" +
                "        if (__dim0 <= 0 || __dim1 <= 0 || __dim2 <= 0) {\n" +
                "            throw new IllegalArgumentException(\"dimensions must be positive\");\n" +
                "        }\n" +
                "        int __l = __dim0 * __dim1 * __dim2;\n" +
                "\n" +
                "        // initialize generated fields\n" +
                "${field_init}\n" +
                "    }\n" +
                "\n" +
                "    // methods\n" +
                "    public int size() {\n" +
                "        return __dim0 * __dim1 * __dim2;\n" +
                "    }\n" +
                "\n" +
                "    public $$Record get(int i) {\n" +
                "        return new $$Record(i);\n" +
                "    }\n" +
                "    public $$Record get(int i0, int i1) {\n" +
                "        return new $$Record(i0 * __dim1 + i1);\n" +
                "    }\n" +
                "    public $$Record get(int i0, int i1, int i2) {\n" +
                "        return new $$Record( (i0 * __dim1 + i1) * __dim2 + i2);\n" +
                "    }\n" +
                "\n" +
                "${setter_0}\n" +
                "\n" +
                "    public void set(int i0, int i1, ${record_type} value) {\n" +
                "        set(i0 * __dim1 + i1, value);\n" +
                "    }\n" +
                "\n" +
                "    public void set(int i0, int i1, int i2, ${record_type} value) {\n" +
                "        set((i0 * __dim1 + i1) * __dim2 + i2, value);\n" +
                "    }\n" +
                "\n" +
                "${record_impl}\n" +
                "\n" +
                "${detached_record_impl}\n" +
                "}\n")
                .replace("${arr_impl_name}", mangledClassName)
                .replace("${record_type}", el.asType().toString())
                .replace("${fields}", pad(arrayImplFieldsStr))
                .replace("${field_init}", pad(arrayImplFieldsInitStr, 8))
                .replace("${setter_0}", pad(arrayImplSetter0))
                .replace("${record_impl}", pad(recordImplStr))
                .replace("${detached_record_impl}", pad(detachedRecordImplStr));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> ret = new HashSet<>();
        ret.add(Record.class.getCanonicalName());
        return ret;
    }
}
