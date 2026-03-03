# Fluentquery Implementation Plan

## 1) Scope and Success Criteria

### MVP scope
- Query API supports: `SELECT`, `WHERE`, `JOIN` (at least `INNER JOIN`), `GROUP BY`, `HAVING`.
- `SELECT` includes custom projections in MVP (not only entity-wide selection).
- `INSERT` and `UPDATE` are out of MVP functional scope.
- Queries are immutable; every mutating operation returns a new query instance.
- SQL output supports:
  - Parameterized mode (default), with ordered parameter collection.
  - Literal/debug mode for inspection.
- Build-time metaclass generation only via Maven plugin.
- Initial dialect support: PostgreSQL and MySQL.
- Join rows may be untyped in MVP (compile-time-safe typed join rows deferred).
- Joins support alias-based disambiguation for self-joins.
- Supported SQL column type mappings include at minimum all types listed in `agents.md`.
- Internal architecture must remain extensible so `INSERT`/`UPDATE` can be added later without breaking `SELECT` APIs.

### Done criteria (MVP)
- A developer can annotate entities and generate `Q*` metaclasses into `<entity package>.fluentquery` via Maven build.
- A generated `QEntities` facade exists per fluentquery package with static accessors for generated Q singletons.
- Example query in `agents.md` can be expressed and rendered for PostgreSQL/MySQL.
- Generated SQL is valid for both dialects for covered features.
- Unit tests cover AST building, immutability behavior, SQL rendering, and plugin generation.
- Integration tests validate plugin + library end-to-end in sample modules.

## 2) Repository and Build Layout

Create a Maven multi-module project with:
- `fluentquery-parent/` (packaging `pom`)
- `fluentquery-lib/` (artifact `fluentquery`)
- `fluentquery-maven-plugin/` (artifact `fluentquery-maven-plugin`)
- `fluentquery-e2e-entities/` (fixture entity module)
- `fluentquery-e2e/` (integration module validating generation + runtime usage)

Parent POM responsibilities:
- Define `groupId`: `com.github.rorymurphy.fluentquery`
- Pin shared dependency/plugin versions.
- Configure Java version and test plugins.

## 3) Architecture Plan

## 3.1 `fluentquery-lib` (query runtime)

### Core model
- Immutable AST nodes:
  - Statement root (`SqlStatement`)
  - Query root (`SelectQuery`) for MVP
  - Clauses (`WhereClause`, `JoinClause`, `GroupByClause`, `HavingClause`)
  - Expressions (`ColumnRef`, `Literal`, `ParameterRef`, `BinaryOp`, `FunctionCall`, etc.)
- Reserve statement variants/interfaces for future DML (`InsertStatement`, `UpdateStatement`) without requiring MVP rendering support.
- Query parameter model:
  - `QueryParameter(index, value, javaType)`
  - Stored in deterministic traversal order during SQL rendering.

### Fluent API surface
- `QueryBuilder.query(QEntityType)` entrypoint.
- Typed predicates and operators (`eq`, `lt`, `lte`, `gt`, `gte`, `in`, `like`, `and`, `or`, etc.).
- Type-restricted operators:
  - `like` only on string-compatible columns.
  - Numeric/date operators only on compatible types.
- `WHERE IN` is expressed via `QColumn.in(...)` inside `where(...)`.

### Typed row strategy
- Base `Query<T>` for entity rows.
- For joins in MVP, use an untyped composite row abstraction with deterministic alias-based field access (`JoinedRow`).
- `JoinedRow.as(alias, type)` is used to disambiguate same-type entries in self-join scenarios.
- Keep internals extensible so typed composite rows can be added in a later phase without AST breakage.

### SQL rendering and dialects
- `SqlDialect` abstraction with implementations:
  - `PostgreSqlDialect`
  - `MySqlDialect`
- `SqlRenderer` returns structured result:
  - SQL string
  - ordered parameter list
- Renderer contracts should be statement-oriented (not `SELECT`-hardcoded) so DML renderers can be added compatibly.
- Provide API for both modes:
  - Parameterized (default)
  - Literal/debug mode (safe, explicit opt-in)

## 3.2 `fluentquery-maven-plugin` (build-time codegen)

### Plugin goals
- Parse compiled classes or source metadata for JPA annotations (`@Table`, `@Column`, `@Id`, `@UniqueConstraint`).
- Generate metaclasses (`QProduct`, etc.) into generated sources.
- Emit debug-level generation logs via `slf4j`.

### Generation output
- One metaclass per entity with typed column descriptors.
- Generated types usable directly by `fluentquery-lib` fluent API.
- Deterministic generated naming and package rules.
- Generated Q classes are emitted into `<entity package>.fluentquery`.
- Each generated package includes a `QEntities` class with static fields named by lower-camel entity class name.

### Plugin integration
- Standard Maven goal (e.g., `generate`) bound to `generate-sources` phase.
- Add generated source directory to compile roots.
- No runtime fallback generation.

## 4) Delivery Phases

## Phase 0: Bootstrap project structure
- Create parent + 2 module POMs.
- Add baseline test setup (JUnit 5) and formatting/lint conventions.
- Add minimal README for module responsibilities.

## Phase 1: Query AST + immutable fluent API
- Implement core AST and immutable `Query<T>` transformations.
- Implement `SELECT`, `WHERE`, `INNER JOIN`, `GROUP BY`, `HAVING` in AST.
- Implement operator/type constraints in API.
- Introduce statement-level extension seams for future `INSERT`/`UPDATE` nodes.

## Phase 2: SQL renderer + dialect abstraction
- Implement renderer visitor over AST.
- Add PostgreSQL/MySQL dialect differences behind `SqlDialect`.
- Implement parameterized + literal rendering APIs.
- Add unit tests for SQL snapshots and parameter ordering.
- Ensure renderer and dialect interfaces are open for future DML methods without signature breaks.

## Phase 3: Maven plugin metaclass generation
- Implement annotation scanning + schema model extraction.
- Implement metaclass code generation templates.
- Wire plugin goal to lifecycle and generated source registration.
- Add debug logs around discovery and file generation.

## Phase 4: End-to-end examples and integration tests
- Add sample entities in test fixture module.
- Run plugin to generate metaclasses during test lifecycle.
- Build representative queries and assert SQL output for both dialects.

## Phase 5: Hardening and docs
- Validate immutability guarantees with focused tests.
- Document public API and plugin configuration.
- Publish MVP usage guide and limitations.

## 5) Testing Strategy

### Unit tests (`fluentquery-lib`)
- AST construction and structural equality.
- Immutability: new object identity after each fluent transformation.
- Type-safe operator coverage (`like`, numeric comparisons, null checks, `in`).
- SQL renderer tests by dialect and feature.
- Compatibility tests that assert statement abstraction can host non-`SELECT` variants (no DML execution required in MVP).
- Self-join safety tests for alias-based joined-row lookup.

### Unit tests (`fluentquery-maven-plugin`)
- Annotation parsing for supported field metadata.
- Generated source content/golden file tests.
- Logging smoke tests at debug level.

### Integration tests
- Full Maven build on sample entities.
- Generated metaclasses compile and execute fluent queries via static imports from `QEntities`.
- SQL + parameter assertions for PostgreSQL/MySQL.

## 6) As-Built Decisions and Changes

- `QueryBuilder` now uses a single entrypoint: `query(instance)`.
- `from(...)` and class-based `query(Class<T>)` were removed.
- Q classes expose singleton fields and alias factories (`as(alias)` / `withAlias(alias)`).
- Static facade `QEntities` enables `import static ...fluentquery.QEntities.*` usage.
- `WHERE IN` implemented via `QColumn.in(Collection)` and `QColumn.in(varargs)`.
- Static `Operators.in(...)` helpers added for static-import parity.
- `QColumn` includes `isNull/isNotNull`, `lte/gte`, and column-to-column comparison overloads.
- Plugin descriptor generation is bound early enough for reactor compile workflows.

## 7) Risks and Mitigations

- **Join API complexity**: keep MVP join rows untyped with alias-based access, and isolate extension points for future typed rows.
- **Dialect drift**: centralize quoting/functions/operators in `SqlDialect`; avoid dialect conditionals in core AST.
- **Codegen brittleness**: use deterministic templates and golden-file verification.
- **Parameter ordering bugs**: enforce single renderer traversal strategy and snapshot tests.

## 8) Initial Backlog (Implementation Order)

1. Parent and module POM scaffolding. ✅
2. Statement abstraction + core AST + immutable query object. ✅
3. Type-safe operator and expression APIs. ✅
4. SQL renderer + PostgreSQL dialect. ✅
5. MySQL dialect adaptations. ✅
6. Maven plugin scanning + `Q*` generation + `QEntities` facade. ✅
7. End-to-end fixture project and integration tests. ✅
8. Documentation and examples. 🔄 (ongoing)

## 9) Open Questions / Clarifications Needed

1. **Literal SQL mode behavior**: Should string literals be fully escaped for executable SQL, or is this mode debug-only and non-executable?
2. **Clause merge semantics**: Should repeated `where(...)` / `having(...)` merge with `AND` or remain replacement semantics?
3. **Typed join rows**: Should a typed join row model be added in MVP+1, or keep alias-based untyped rows for now?

## 10) Confirmed Decisions (Captured)

- **JOIN typing model**: MVP uses untyped join rows.
- **`SELECT` scope**: MVP includes custom projections.
- **Plugin discovery source**: Maven plugin scans compiled classes.
- **Annotation baseline**: MVP targets `javax.persistence`.
- **Generated package strategy**: Q classes are generated into `<entity package>.fluentquery` with package-level `QEntities` facade.
- **IN API location**: `IN` is modeled on `QColumn.in(...)` (not as a `Query` shortcut).

## 11) Suggested Defaults (if not specified)

- Untyped join row model in MVP, with extensibility hooks for typed row variants later.
- `SELECT` supports custom projections in MVP.
- Literal mode is debug-oriented, clearly marked unsafe for untrusted input.
- Plugin scans compiled classes during build and supports `javax.persistence`.
- `Q*` classes generated in `<entity package>.fluentquery`.
- `QEntities` facade generated per fluentquery package.
- `eq(null)` becomes `IS NULL`.
- Java 21+ and latest stable Maven.

## 12) Execution Backlog with Effort Estimates

Estimates assume one engineer full-time and include implementation + tests.

### Epic A: Project scaffolding and build baseline (1.0-1.5 days)
1. Create parent/module structure and baseline POMs (0.5d)
2. Configure Java toolchain, Surefire/Failsafe, and dependency management (0.5d)
3. Add minimal module READMEs and build verification (`mvn test`) (0.5d)

### Epic B: Core query model and fluent API (3.0-4.0 days)
1. Implement statement abstraction and immutable query AST types with copy-on-write transformations (1.0-1.5d)
2. Add expression model (`ColumnRef`, `Literal`, `ParameterRef`, `BinaryOp`, `FunctionCall`) (1.0d)
3. Implement fluent entrypoints and operators (`eq`, `lt`, `like`, `and`, `or`) with type constraints (1.0d)
4. Implement JOIN support with untyped joined row access model (0.5-1.0d)

### Epic C: SQL rendering and dialect support (2.5-3.5 days)
1. Implement statement-oriented renderer visitor and deterministic parameter extraction (1.0d)
2. Implement PostgreSQL dialect behaviors (quoting, placeholders, syntax) (0.5-1.0d)
3. Implement MySQL dialect behaviors and compatibility tests (0.5-1.0d)
4. Expose parameterized and literal/debug output APIs (0.5d)

### Epic D: Maven plugin metaclass generation (3.0-4.0 days)
1. Implement compiled-class scanning for `javax.persistence` annotations (1.0d)
2. Build entity metadata model and validation checks (0.5-1.0d)
3. Generate deterministic `Q*` sources in entity package (1.0d)
4. Register generated sources and bind plugin goal to lifecycle with debug logging (0.5-1.0d)

### Epic E: End-to-end tests, examples, and docs (2.0-3.0 days)
1. Add fixture entities spanning required SQL type mappings (0.5-1.0d)
2. Add integration tests for plugin + library + SQL rendering across dialects (1.0d)
3. Add usage examples and MVP limitations documentation (0.5-1.0d)

**Estimated total**: 12.0-16.5 engineer-days.

## 13) Milestone Timeline

### Milestone M1 (end of week 1)
- Epics A + B complete.
- Unit tests pass for AST immutability and operator typing.

### Milestone M2 (mid week 2)
- Epic C complete.
- SQL snapshot tests pass for PostgreSQL/MySQL in parameterized mode.

### Milestone M3 (end of week 2)
- Epic D complete.
- Plugin generates compiling `Q*` classes from compiled entities.

### Milestone M4 (week 3)
- Epic E complete.
- End-to-end fixtures and documentation finalized for MVP release candidate.

### Current status
- Core implementation and tests are complete and passing.
- Documentation alignment (plan/design/docs) is the remaining primary workstream.

## 14) Immediate Next Tasks

1. Keep design and implementation-plan docs synchronized with as-built API.
2. Add additional e2e coverage for multi-join scenarios and clause semantics.
3. Decide and implement `where`/`having` merge behavior (`AND` chaining vs replacement).
4. Define and prioritize MVP+1 typed join-row roadmap.
