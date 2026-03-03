package com.github.rorymurphy.fluentquery.maven.generate;

import com.github.rorymurphy.fluentquery.maven.model.ColumnMeta;
import com.github.rorymurphy.fluentquery.maven.model.EntityMeta;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaClassGeneratorTest {
    @Test
    void generatesExpectedQClassShape() {
        EntityMeta entityMeta = new EntityMeta(
            "com.example",
            "Product",
            "product",
            List.of(
                new ColumnMeta("id", "id", "java.lang.Integer", false),
                new ColumnMeta("price", "price", "java.math.BigDecimal", false),
                new ColumnMeta("productUuid", "product_uuid", "java.util.UUID", false)
            )
        );

        String source = new MetaClassGenerator().generate(entityMeta);

        assertTrue(source.contains("package com.example.fluentquery;"));
        assertTrue(source.contains("import com.example.Product;"));
        assertTrue(source.contains("class QProduct extends QEntity<Product>"));
        assertTrue(source.contains("public static final QProduct product = new QProduct();"));
        assertTrue(source.contains("public static QProduct as(String alias)"));
        assertTrue(source.contains("public static QProduct withAlias(String alias)"));
        assertTrue(source.contains("public QProduct(String alias)"));
        assertTrue(source.contains("QColumn<Product, java.math.BigDecimal> price"));
        assertTrue(source.contains("SqlType.DECIMAL"));
        assertTrue(source.contains("SqlType.UUID"));
    }

    @Test
    void generatesQEntitiesFacade() {
        List<EntityMeta> entities = List.of(
            new EntityMeta("com.example", "Product", "product", List.of()),
            new EntityMeta("com.example", "Purchase", "purchase", List.of())
        );

        String source = new MetaClassGenerator().generateQEntities("com.example", entities);

        assertTrue(source.contains("package com.example.fluentquery;"));
        assertTrue(source.contains("public final class QEntities"));
        assertTrue(source.contains("public static final QProduct product = QProduct.product;"));
        assertTrue(source.contains("public static final QPurchase purchase = QPurchase.purchase;"));
    }
}
