package com.github.rorymurphy.fluentquery.maven.generate;

import com.github.rorymurphy.fluentquery.maven.model.ColumnMeta;
import com.github.rorymurphy.fluentquery.maven.model.EntityMeta;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MetaClassGenerator {
    public String generatedPackage(String entityPackage) {
        return entityPackage + ".fluentquery";
    }

    public String generate(EntityMeta entityMeta) {
        String qClassName = "Q" + entityMeta.className();
        String singletonName = lowerCamel(entityMeta.className());
        String generatedPackage = generatedPackage(entityMeta.packageName());
        String entityFqn = entityMeta.packageName() + "." + entityMeta.className();
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(generatedPackage).append(";\n\n")
            .append("import ").append(entityFqn).append(";\n")
            .append("import com.github.rorymurphy.fluentquery.meta.QColumn;\n")
            .append("import com.github.rorymurphy.fluentquery.meta.QEntity;\n")
            .append("import com.github.rorymurphy.fluentquery.types.SqlType;\n")
            .append("import java.util.List;\n\n")
            .append("public final class ").append(qClassName).append(" extends QEntity<").append(entityMeta.className()).append("> {\n")
            .append("    public static final ").append(qClassName).append(" ").append(singletonName).append(" = new ").append(qClassName).append("();\n");

        for (ColumnMeta column : entityMeta.columns()) {
            String javaTypeRef = javaTypeReference(column.javaType());
            builder.append("    public final QColumn<")
                .append(entityMeta.className())
                .append(", ")
                .append(javaTypeRef)
                .append("> ")
                .append(column.fieldName())
                .append(";\n");
        }

        builder.append("\n    public static ").append(qClassName).append(" as(String alias) {\n")
            .append("        return new ").append(qClassName).append("(alias);\n")
            .append("    }\n\n")
            .append("    public static ").append(qClassName).append(" withAlias(String alias) {\n")
            .append("        return new ").append(qClassName).append("(alias);\n")
            .append("    }\n\n")
            .append("    public ").append(qClassName).append("() {\n")
            .append("        this(\"").append(defaultAlias(entityMeta.className())).append("\");\n")
            .append("    }\n\n")
            .append("    public ").append(qClassName).append("(String alias) {\n")
            .append("        super(")
            .append(entityMeta.className())
            .append(".class, \"")
            .append(entityMeta.tableName())
            .append("\", alias, List.of());\n");

        for (ColumnMeta column : entityMeta.columns()) {
            String javaTypeRef = javaTypeReference(column.javaType());
            builder.append("        this.")
                .append(column.fieldName())
                .append(" = new QColumn<>(this, \"")
                .append(column.columnName())
                .append("\", ")
                .append(javaTypeRef)
                .append(".class, SqlType.")
                .append(mapSqlType(column.javaType()))
                .append(", ")
                .append(column.nullable())
                .append(");\n");
        }

        builder.append("    }\n")
            .append("}\n");
        return builder.toString();
    }

    public String generateQEntities(String entityPackage, List<EntityMeta> entities) {
        String generatedPackage = generatedPackage(entityPackage);
        List<EntityMeta> sorted = new ArrayList<>(entities);
        sorted.sort(Comparator.comparing(EntityMeta::className));

        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(generatedPackage).append(";\n\n")
            .append("public final class QEntities {\n")
            .append("    private QEntities() {\n")
            .append("    }\n\n");

        for (EntityMeta entityMeta : sorted) {
            String qClassName = "Q" + entityMeta.className();
            String fieldName = lowerCamel(entityMeta.className());
            builder.append("    public static final ")
                .append(qClassName)
                .append(" ")
                .append(fieldName)
                .append(" = ")
                .append(qClassName)
                .append(".")
                .append(fieldName)
                .append(";\n");
        }

        builder.append("}\n");
        return builder.toString();
    }

    private static String javaTypeReference(String typeName) {
        return switch (typeName) {
            case "int" -> "Integer";
            case "long" -> "Long";
            case "double" -> "Double";
            case "float" -> "Float";
            case "boolean" -> "Boolean";
            case "byte" -> "Byte";
            case "short" -> "Short";
            case "char" -> "Character";
            default -> typeName;
        };
    }

    private static String defaultAlias(String className) {
        return className.substring(0, 1).toLowerCase();
    }

    private static String lowerCamel(String className) {
        if (className == null || className.isBlank()) {
            return "q";
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private static String mapSqlType(String javaTypeName) {
        return switch (javaTypeName) {
            case "int", "java.lang.Integer", "long", "java.lang.Long", "short", "java.lang.Short", "byte", "java.lang.Byte" -> "INT";
            case "java.math.BigDecimal" -> "DECIMAL";
            case "float", "java.lang.Float", "double", "java.lang.Double" -> "FLOAT";
            case "char", "java.lang.Character" -> "CHAR";
            case "java.lang.String" -> "VARCHAR";
            case "java.time.LocalDate" -> "DATE";
            case "java.time.LocalTime" -> "TIME";
            case "java.time.LocalDateTime" -> "DATETIME";
            case "java.time.Instant", "java.sql.Timestamp" -> "TIMESTAMP";
            case "boolean", "java.lang.Boolean" -> "BOOLEAN";
            case "java.util.UUID" -> "UUID";
            case "[B" -> "VARBINARY";
            default -> "VARCHAR";
        };
    }
}
