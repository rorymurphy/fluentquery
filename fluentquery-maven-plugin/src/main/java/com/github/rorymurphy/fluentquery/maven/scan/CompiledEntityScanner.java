package com.github.rorymurphy.fluentquery.maven.scan;

import com.github.rorymurphy.fluentquery.maven.model.ColumnMeta;
import com.github.rorymurphy.fluentquery.maven.model.EntityMeta;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Table;

public final class CompiledEntityScanner {
    public List<EntityMeta> scan(File classesDirectory, List<String> classpathElements) throws Exception {
        if (!classesDirectory.exists()) {
            return List.of();
        }

        List<URL> urls = new ArrayList<>();
        urls.add(classesDirectory.toURI().toURL());
        for (String classpathElement : classpathElements) {
            urls.add(new File(classpathElement).toURI().toURL());
        }

        try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader())) {
            List<String> classNames = Files.walk(classesDirectory.toPath())
                .filter(path -> path.toString().endsWith(".class"))
                .map(path -> classesDirectory.toPath().relativize(path).toString())
                .map(name -> name.replace(File.separatorChar, '.'))
                .map(name -> name.substring(0, name.length() - ".class".length()))
                .filter(name -> !name.contains("$"))
                .collect(Collectors.toList());

            List<EntityMeta> entities = new ArrayList<>();
            for (String className : classNames) {
                Class<?> type = classLoader.loadClass(className);
                Table table = type.getAnnotation(Table.class);
                if (table == null) {
                    continue;
                }
                List<ColumnMeta> columns = new ArrayList<>();
                for (Field field : type.getDeclaredFields()) {
                    Column column = field.getAnnotation(Column.class);
                    if (column != null) {
                        String columnName = column.name().isBlank() ? field.getName() : column.name();
                        columns.add(new ColumnMeta(field.getName(), columnName, field.getType().getName(), column.nullable()));
                    }
                }
                String packageName = type.getPackageName();
                entities.add(new EntityMeta(packageName, type.getSimpleName(), table.name(), columns));
            }
            return entities;
        }
    }
}
