package com.github.rorymurphy.fluentquery.e2e.entities.fluentquery;

import com.github.rorymurphy.fluentquery.e2e.entities.EmployeeEntity;
import com.github.rorymurphy.fluentquery.meta.QColumn;
import com.github.rorymurphy.fluentquery.meta.QEntity;
import com.github.rorymurphy.fluentquery.types.SqlType;
import java.util.List;

public final class QEmployeeEntity extends QEntity<EmployeeEntity> {
    public static final QEmployeeEntity employeeEntity = new QEmployeeEntity();
    public final QColumn<EmployeeEntity, java.lang.Integer> id;
    public final QColumn<EmployeeEntity, java.lang.String> name;
    public final QColumn<EmployeeEntity, java.lang.Integer> managerId;

    public static QEmployeeEntity as(String alias) {
        return new QEmployeeEntity(alias);
    }

    public static QEmployeeEntity withAlias(String alias) {
        return new QEmployeeEntity(alias);
    }

    public QEmployeeEntity() {
        this("e");
    }

    public QEmployeeEntity(String alias) {
        super(EmployeeEntity.class, "employee", alias, List.of());
        this.id = new QColumn<>(this, "id", java.lang.Integer.class, SqlType.INT, false);
        this.name = new QColumn<>(this, "name", java.lang.String.class, SqlType.VARCHAR, false);
        this.managerId = new QColumn<>(this, "manager_id", java.lang.Integer.class, SqlType.INT, true);
    }
}
