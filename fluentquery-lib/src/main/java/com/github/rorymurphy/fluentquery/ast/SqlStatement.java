package com.github.rorymurphy.fluentquery.ast;

public sealed interface SqlStatement permits SelectStatement, InsertStatement, UpdateStatement {
}
