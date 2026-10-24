# 작업 목록: 파일 형식 표시 이름 충돌 수정

**입력**: [명세](spec.md), [계획](plan.md) 및 설계 문서

## 1단계: 준비

- [X] T001 .gitignore 및 두 파일 형식의 기존 구현과 src/test/kotlin/com/look4counter/EditorFileTypeTest.kt 검증 환경을 확인한다.

## 2단계: 기반 확인

새 기반 구성은 필요하지 않다. T001 완료 후 사용자 스토리를 수행한다.

## 3단계: 사용자 스토리 1 — 표시 이름 구분

**독립 검증**: EditorFileTypeTest에서 기본 XML과 두 Glue 표시 이름의 유일성과 기존 XML 동작을 확인한다.

- [X] T002 [US1] src/test/kotlin/com/look4counter/EditorFileTypeTest.kt에 표시 이름 계약 검증을 추가하고 수정 전 실패를 확인한다. (FR-001, SC-001)
- [X] T003 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/GlueSqlFileType.kt와 modeleditor/src/main/kotlin/com/look4counter/modeleditor/GlueUmlAdFileType.kt에서 표시 이름을 기존 이름으로 재정의한다. (FR-001, FR-002)
- [X] T004 [US1] src/test/kotlin/com/look4counter/EditorFileTypeTest.kt 전체를 실행하여 기존 연결 및 XML 편집 동작과 새 계약을 검증한다. (FR-002, SC-001, SC-002)

## 4단계: 마무리

- [X] T005 git diff --check를 실행하고 specs/004-fix-filetype-display-names/quickstart.md에 검증 결과를 기록한다.

## 의존성과 구현 전략

T001 → T002 → T003 → T004 → T005 순서로 진행한다. 유일한 사용자 스토리가 최소 완성 범위이다. T003의 두 파일 수정은 독립적이므로 병렬 가능하지만 실제 작업은 순차 수행한다. 테스트를 먼저 실패시킨 뒤 구현하고 전체 관련 테스트를 확인한다.
