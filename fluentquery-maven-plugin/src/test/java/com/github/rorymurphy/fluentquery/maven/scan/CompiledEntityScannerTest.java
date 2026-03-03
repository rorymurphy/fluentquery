package com.github.rorymurphy.fluentquery.maven.scan;

import com.github.rorymurphy.fluentquery.maven.model.EntityMeta;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompiledEntityScannerTest {
    @Test
    void findsJpaAnnotatedEntitiesInCompiledTestOutput() throws Exception {
        File testClasses = new File("target/test-classes");
        List<String> classpath = List.of(testClasses.getAbsolutePath());

        List<EntityMeta> entities = new CompiledEntityScanner().scan(testClasses, classpath);

        EntityMeta product = entities.stream()
            .filter(meta -> meta.className().equals("ProductEntity"))
            .findFirst()
            .orElseThrow();

        assertEquals("product", product.tableName());
        assertFalse(product.columns().isEmpty());
        assertTrue(product.columns().stream().anyMatch(column -> column.columnName().equals("product_uuid")));
    }
}
