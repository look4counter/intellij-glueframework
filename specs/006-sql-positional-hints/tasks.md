# 작업 목록: SQL 위치 파라미터 힌트

## 1단계: 준비

- [x] T001 specs/006-sql-positional-hints/spec.md와 품질 점검 및 설계 확인

## 2단계: 기반

- [x] T002 queryeditor/src/main/kotlin/com/look4counter/queryeditor/QuerySqlEditor.kt와 sql/SqlLexer.kt의 편집기·토큰 수명 확인

## 3단계: 사용자 이야기 1

- [x] T003 [US1] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 ? 색상·번호·제외 영역·편집·전환·저장·실행 취소 검증 추가 (FR-001~005)
- [x] T004 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlLexer.kt에서 ?를 파라미터로 분류 (FR-001·FR-003)
- [x] T005 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlParameterHints.kt를 추가하여 기본 IntelliJ 번호 인레이와 문서 변경 동기화 구현 (FR-002~005)
- [x] T006 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QuerySqlEditor.kt에서 힌트와 해제 수명 연결 (FR-004~005)

## 4단계: 검증

- [x] T007 관련 테스트와 UI 렌더링을 확인하고 specs/006-sql-positional-hints/quickstart.md에 결과 기록 및 수렴 점검

## 의존성과 실행 전략

T001 → T002 → T003 → T004 → T005 → T006 → T007 순서다.
단일 이야기 전체가 제공 범위이며 토큰 색상과 힌트·문서 보존을 독립 검증한다.
lexer 수정과 힌트 구현은 별도 파일로 병렬 가능하나 이번에는 순차 실행한다.




## 5단계: 0부터 시작하는 순번

- [x] T008 [US1] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt의 순번·테마·편집 기대값을 0부터 시작하도록 변경 (FR-002·FR-004)
- [x] T009 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlParameterHints.kt에서 번호를 0부터 증가시키도록 수정 (FR-002)
- [x] T010 관련 회귀 및 렌더링을 확인하고 specs/006-sql-positional-hints/quickstart.md에 결과 기록

후속 순번 수정은 T008 → T009 → T010 순서로 진행한다.

