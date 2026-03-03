package com.github.rorymurphy.fluentquery.e2e.entities.fluentquery;

import com.github.rorymurphy.fluentquery.e2e.entities.ProductEntity;
import com.github.rorymurphy.fluentquery.meta.QColumn;
import com.github.rorymurphy.fluentquery.meta.QEntity;
import com.github.rorymurphy.fluentquery.types.SqlType;
import java.util.List;

public final class QProductEntity extends QEntity<ProductEntity> {
    public static final QProductEntity productEntity = new QProductEntity();
    public final QColumn<ProductEntity, java.lang.Integer> id;
    public final QColumn<ProductEntity, java.lang.String> name;
    public final QColumn<ProductEntity, java.lang.String> description;
    public final QColumn<ProductEntity, java.math.BigDecimal> price;

    public static QProductEntity as(String alias) {
        return new QProductEntity(alias);
    }

    public static QProductEntity withAlias(String alias) {
        return new QProductEntity(alias);
    }

    public QProductEntity() {
        this("p");
    }

    public QProductEntity(String alias) {
        super(ProductEntity.class, "product", alias, List.of());
        this.id = new QColumn<>(this, "id", java.lang.Integer.class, SqlType.INT, false);
        this.name = new QColumn<>(this, "name", java.lang.String.class, SqlType.VARCHAR, false);
        this.description = new QColumn<>(this, "description", java.lang.String.class, SqlType.VARCHAR, true);
        this.price = new QColumn<>(this, "price", java.math.BigDecimal.class, SqlType.DECIMAL, false);
    }
}
