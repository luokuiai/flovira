## 1. Core Business Correlation

- [x] 1.1 Add `businessType` to the instance contract and explicit business-type start overloads
- [x] 1.2 Persist the selected business type during instance creation while keeping old start APIs compatible
- [x] 1.3 Add business-key instance, current-task and history-task service queries

## 2. ORM Alignment

- [x] 2.1 Add `businessType` to MyBatis, MyBatis-Plus and Easy-Query instance entities and query conditions
- [x] 2.2 Add batch instance-ID task and history queries to all three ORM implementations
- [x] 2.3 Update MyBatis mapper interfaces and XML mappings for the new field and batch queries

## 3. Database Scripts

- [x] 3.1 Add the instance business type and business-key index to MySQL and PostgreSQL V1 scripts
- [x] 3.2 Add the same instance field/index to Oracle and SQL Server scripts
- [x] 3.3 Add tenant/instance/time history lookup indexes without adding business columns to task tables

## 4. Verification

- [x] 4.1 Add focused core tests for explicit/default business types and business-key task/history lookup
- [x] 4.2 Compile core, all ORM core modules and Spring wiring; validate mapper XML, SQL consistency and OpenSpec
