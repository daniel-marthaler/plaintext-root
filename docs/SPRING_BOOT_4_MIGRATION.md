# Spring Boot 4 Migration Analysis

## Current State

| Component | Current Version |
|-----------|----------------|
| Spring Boot | 3.5.11 |
| Java | 25 |
| Jakarta EE | 10 (Jakarta Faces 4.1) |
| Spring Security | 6.x |
| Hibernate | 6.6.x |
| JoinFaces | 5.5.8 |

## Spring Boot 4 Requirements (Expected)

Based on Spring Framework 7 roadmap and Spring Boot 4 previews:

| Requirement | Status | Notes |
|-------------|--------|-------|
| Java 17+ (likely 21+) | OK | Currently using Java 25 |
| Jakarta EE 10+ | OK | Already migrated from javax to jakarta |
| Virtual Threads | Ready | Java 25 supports virtual threads |
| AOT Compilation | Partial | Would need GraalVM testing |
| Structured Concurrency | Optional | Java 25 supports preview features |

## Migration Feasibility

### Low Risk (Already Compatible)

1. **Jakarta EE Migration**: Already done. All imports use `jakarta.*` namespace.
2. **Spring Security**: Using modern `SecurityFilterChain` pattern, no deprecated `WebSecurityConfigurerAdapter`.
3. **Java Version**: Java 25 exceeds any SB4 requirement.
4. **JPA/Hibernate**: Using Hibernate 6.x which aligns with SB4.

### Medium Risk (Needs Attention)

1. **JoinFaces Compatibility**
   - JoinFaces must release a Spring Boot 4 compatible version
   - This is the **biggest blocker** — JSF integration depends entirely on JoinFaces
   - Alternative: Direct JSF servlet registration without JoinFaces
   - **Risk**: High — if JoinFaces lags behind, migration is blocked

2. **Circular References**
   - `application.yml` has `allow-circular-references: true`
   - Spring Boot 4 may remove this option entirely
   - **Action needed**: Resolve circular dependencies before migration
   - **Effort**: 2-3 days

3. **Bean Definition Overriding**
   - `allow-bean-definition-overriding: true` is used
   - May not be available in SB4
   - **Action needed**: Use `@Primary` or `@ConditionalOnMissingBean` instead

4. **Deprecated APIs**
   - `SimpleDateFormat` usage in `TimeUtil.java` (thread-unsafe)
   - Joda-Time dependency (deprecated since Java 8)
   - **Action needed**: Migrate to `java.time` API

### High Risk (May Block Migration)

1. **JoinFaces / JSF Integration**
   - Central to the entire UI layer
   - No confirmed SB4 support timeline
   - **Mitigation**: Monitor JoinFaces releases, consider fallback plan

2. **PrimeFaces Compatibility**
   - PrimeFaces 15.x should work with SB4 (Jakarta Faces 4.x based)
   - But needs verification with Spring Boot 4's new servlet handling

3. **Flyway**
   - Flyway typically supports new Spring Boot versions quickly
   - Low risk but needs version bump verification

## Dependency Compatibility Matrix

| Dependency | Current | SB4 Compatible? | Notes |
|-----------|---------|-----------------|-------|
| JoinFaces | 5.5.8 | Unknown | **Blocker** — must wait for release |
| PrimeFaces | 15.0.10 | Likely | Jakarta Faces 4.x based |
| Hibernate | 6.6.x | Yes | SB4 will use Hibernate 6.x/7.x |
| Flyway | (managed) | Yes | Quick updates historically |
| Lombok | 1.18.44 | Yes | Java 25 compatible |
| XStream | 1.4.21 | Yes | No Spring dependency |
| Eclipse Paho (MQTT) | 1.2.5 | Yes | No Spring dependency |
| JJWT | 0.13.0 | Yes | No Spring dependency |
| BouncyCastle | 1.83 | Yes | No Spring dependency |

## Recommended Migration Path

### Phase 0: Pre-Migration (Do Now)
1. Remove `allow-circular-references: true`
2. Remove `allow-bean-definition-overriding: true`
3. Migrate `TimeUtil` from Joda-Time to `java.time`
4. Replace `SimpleDateFormat` with `DateTimeFormatter`
5. Add deprecation warnings for future removals

### Phase 1: Wait for JoinFaces (Blocking)
1. Monitor JoinFaces for Spring Boot 4 support
2. Test with Spring Boot 4 milestones when available
3. Prepare JSF fallback plan (direct servlet registration)

### Phase 2: Migration
1. Bump Spring Boot parent version
2. Update JoinFaces to SB4-compatible version
3. Fix any breaking API changes
4. Run full test suite
5. Test all JSF pages manually

### Phase 3: Leverage New Features
1. Enable Virtual Threads for better scalability
2. Evaluate AOT compilation for faster startup
3. Consider structured concurrency for discovery module

## Effort Estimate

| Phase | Effort | Timeline |
|-------|--------|----------|
| Phase 0 (Pre-Migration) | 3-5 days | Can start now |
| Phase 1 (Wait) | 0 days | Depends on JoinFaces |
| Phase 2 (Migration) | 5-10 days | After JoinFaces release |
| Phase 3 (New Features) | 3-5 days | Optional, post-migration |

## Conclusion

Spring Boot 4 migration is **feasible but blocked by JoinFaces**. The codebase is well-prepared (Jakarta EE, modern Spring Security, Java 25). The main pre-migration work is resolving circular dependencies and modernizing date/time utilities. Estimated total effort: **2-3 weeks** once JoinFaces supports SB4.
