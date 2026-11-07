# 실행 작업: QueryEditor SQL 포매팅

## 1단계: 준비

- [x] T001 specs/027-query-sql-format/spec.md와 설계·명확화·품질 점검을 검토한다.

## 2단계: 기반

- [x] T002 specs/027-query-sql-format/plan.md·tasks.md를 speckit-analyze로 검토한다.
- [x] T003 src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 포맷 버튼과 선택 쿼리 Source 반영·Undo/Redo 검증을 추가하고 기존 구현의 실패를 확인한다. FR-004~005.

## 3단계: 사용자 이야기 1

독립 검증: 사용자 기준 결과가 정확히 일치하고 반복 적용·토큰 보존·오류 보호가 통과한다.

- [x] T004 [US1] src/test/kotlin/com/look4counter/SqlFormatterTest.kt에 기준 예제·멱등성·CTE·CASE·조건·함수·주석·문자열·과도한 입력 검증을 작성한다. FR-001~003, FR-007~008.
- [x] T005 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormattingTokens.kt와 SqlFormatter.kt에 보존 토큰·안전 검사·정렬 작성기를 구현한다. FR-001~003, FR-007.

## 4단계: 사용자 이야기 2

독립 검증: 버튼·단축키와 단일 Undo/Redo·저장·쓰기 보호·선택 변경 검증이 통과한다.

- [x] T006 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에 SQL 포맷 버튼·코드 정리 단축키·선택 상태 및 쓰기 문맥 보호·단일 모델 반영을 구현한다. FR-004~007.
- [x] T007 [US2] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 읽기 전용·혼합 본문·잘못된 SQL·잘못된 XML·폐기·지연 중 선택 변경·저장·반복 적용 보호를 추가하고 통합 검증한다. FR-004~008, SC-002.

## 5단계: 검증과 마무리

- [x] T008 README.md와 specs/027-query-sql-format/quickstart.md에 규칙·사용법·제한·검증 결과를 기록하고 전체 test·buildPlugin·verifyPluginStructure를 실행한다. FR-008, SC-001~003.
- [x] T009 specs/027-query-sql-format/spec.md·plan.md·tasks.md와 구현을 speckit-converge로 대조하고 남은 작업을 완료한다.

## 의존성과 실행 전략

T001→T002→T003→T004→T005→T006→T007→T008→T009 순서다. US1의 순수 포매팅을 먼저 완성한 뒤 US2 편집기 반영을 수행한다. 순수·통합 테스트는 독립 파일이지만 이번 작업은 순차 실행한다. 기존 문맥 자동완성 검증은 유지한다.

## 6단계: 버튼 제거와 DML 확장

- [x] T010 queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt의 포맷 버튼을 제거하고 단축키와 보호 조건을 유지한다. FR-009.
- [x] T011 queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormatter.kt에 UPDATE·INSERT·MERGE 기준 열과 앞쪽 콤마를 구현하고 SqlFormatterTest.kt·QuerySqlEditorTest.kt를 갱신한다. FR-009~010.
- [x] T012 README.md와 quickstart.md를 갱신하고 전체 테스트·ZIP·구조 검증 후 수렴을 확인한다. FR-008~010.

후속 순서: T010→T011→T012. 기존 버튼 요구는 FR-009로 대체한다.

## 7단계: 주석 및 선택 포맷

- [x] T013 SqlFormattingTokens.kt·SqlFormatter.kt에 단독 주석 줄 보존을 구현하고 SqlFormatterTest.kt에서 검증한다. FR-011.
- [x] T014 QueryDesignEditor.kt에 선택 범위와 stamp 보호·부분 교체를 구현하고 QuerySqlEditorTest.kt에서 검증한다. FR-012.
- [x] T015 전체 테스트·빌드·문서·수렴 검증을 수행한다. FR-008~012.

## 8단계: 예제 기반 배치 보완

- [x] T016 SqlFormatterTest.kt의 최신 세 예제 정확 결과와 멱등성 검증을 추가·갱신한다. FR-013~015.
- [x] T017 SqlFormatter.kt의 서브쿼리 괄호 열·INSERT 헤더 및 목록·MERGE 분기를 구현한다. FR-013~015.
- [x] T018 전체 테스트·빌드·문서 갱신 후 수렴을 점검한다. FR-008, FR-011~015.

기존 품질 점검은 유지하며 최신 예제가 이전 배치 요구를 대체한다. 작업 분석에서 모든 후속 요구가 세 작업에 대응하며 중요한 모호함·충돌은 없다.

## 9단계: PIVOT·UNPIVOT

- [x] T019 SqlFormatterTest.kt에 사용자 PIVOT 예제와 UNPIVOT·튜플·함수·멱등성 검증을 추가한다. FR-016~017.
- [x] T020 SqlFormatter.kt에 피벗 구역·본문·IN 목록 및 인접 FROM 서브쿼리 배치를 구현한다. FR-016~017.
- [x] T021 전체 테스트·ZIP·구조 검증과 문서 갱신 및 수렴 점검을 수행한다. FR-008, FR-011~017.

후속 요구와 세 작업 대응을 분석했으며 기존 주석·선택 보호와 충돌이 없다. 명세 품질 점검은 그대로 통과한다.
