# 작업 목록: 추적 출처 이동과 탐색 기록

## 1단계: 기반

- [x] T001 specs/037-trace-source-navigation/spec.md·plan.md·research.md·data-model.md·contracts/navigation.md에 명확화·설계·계약을 작성한다.

## 2단계: 직전 출처 이동

- [x] T002 [US1] src/test/kotlin/com/look4counter/SqlTraceNavigationTest.kt에 컬럼·CTE·테이블 한정자·복수 입력·무출처 회귀를 작성한다. FR-001, FR-002.
- [x] T003 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlTrace.kt에 직접 출처 후보 해석을 구현한다. FR-001, FR-002.
- [x] T004 [US1] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 Ctrl+클릭 이동·일반 클릭 무시·SQL 보존 회귀를 작성한다. FR-003, FR-006, FR-007.
- [x] T005 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt에 수명·스냅샷 검증을 갖춘 비동기 클릭 및 복수 후보 팝업을 구현한다. FR-001~FR-003, FR-005, FR-006.

## 3단계: 이동 기록

- [x] T006 [US2] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 뒤로·앞으로·분기·트리·문서 변경·쿼리 격리 회귀를 작성한다. FR-004~FR-007.
- [x] T007 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt에 문서별 제한된 기록과 뒤로·앞으로 버튼 및 기존 트리 이동 기록을 구현한다. FR-004~FR-006.

## 4단계: 검증과 마무리

- [x] T008 README.md에 사용법을 추가하고 specs/037-trace-source-navigation/quickstart.md에 SQL 추적·편집기 회귀 결과를 기록한다. FR-007, SC-001~SC-003.

의존성: T001 → T002 → T003 → T004 → T006 → T005 → T007 → T008. 같은 파일의 변경이 있으므로 순차 수행한다. 두 이야기는 각각 출처 이동과 기록 이동으로 검증하며 최소 제공 범위는 둘 모두다. 다른 파일의 T002와 UI 회귀 작성은 병렬 가능하지만 이번 작업은 순차 진행한다. 구현 후 speckit-converge로 요구사항과 작업을 대조한다.

## 5단계: Ctrl 링크와 기본 탐색 충돌 보정

- [x] T009 [US3] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 IDE 전체 Ctrl 클릭·밑줄·키 해제·이탈·수정 무효화 및 이동 기록 회귀를 작성한다. FR-008, FR-009, FR-013.
- [x] T010 [US3] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlTrace.kt·QueryColumnTracePanel.kt에 공통 링크 범위와 전용 마우스 액션을 구현한다. FR-008, FR-009.
- [x] T011 [US3] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryTraceHover.kt에 수명·비동기 검증을 갖춘 hover 밑줄과 커서를 구현한다. FR-008, FR-012.

## 6단계: 동명 컬럼 추적과 회색 강조

- [x] T012 [US4] src/test/kotlin/com/look4counter/SqlColumnTraceTest.kt·SqlTraceNavigationTest.kt에 이름 변경 경계·동일 이름 연결·강조 범위·소스 격리 회귀를 작성하고 이전 별칭 전달 기대를 최신 계약으로 갱신한다. FR-010, FR-011, FR-013.
- [x] T013 [US4] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlColumnTrace.kt에서 컬럼 이름별 간선 제한과 출력·참조·조건절 강조 범위를 계산한다. FR-010, FR-011.
- [x] T014 [US4] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt에 컬럼 회색 배경과 무효화 처리를 구현하고 QuerySqlEditorTest.kt에 실제 범위 회귀를 추가한다. FR-011~FR-013.
- [x] T015 README.md·specs/037-trace-source-navigation/quickstart.md·contracts/navigation.md에 최신 계약과 전체 회귀 결과를 기록한다. SC-004.

T009 → T012 → T013 → T010 → T011 → T014 → T015 순서로 진행한다. 공통 파일이 있어 순차 수행하며 최소 제공 범위는 사용자 이야기 3·4 전체다. 최신 요구는 이전의 이름 변경 전달 기대를 대체한다.

## 7단계: 테이블의 동명 경계와 회색 강조

- [x] T016 [US5] src/test/kotlin/com/look4counter/SqlTableTraceTest.kt에 테이블 이름 변경 경계·CTE 참조·정확한 회색 범위·바인딩·인용·스키마·문장 격리 회귀를 작성하고 이전 계보 기대를 갱신한다. FR-014~FR-016, FR-018.
- [x] T017 [US5] src/test/kotlin/com/look4counter/SqlTraceNavigationTest.kt·QuerySqlEditorTest.kt에 테이블 경계에서 Ctrl 이동 중단·회색 표시·쿼리 전환 및 기존 트리 수명 회귀를 작성하고 새 동명 기준으로 갱신한다. FR-017, FR-018.
- [x] T018 [US5] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlTableTrace.kt에 테이블 경로별 간선 제한과 선언·별칭·한정자 범위 수집을 구현한다. FR-014~FR-017.

## 8단계: 탐색 버튼 제거와 검증

- [x] T019 [US6] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt에서 탐색 버튼 영역을 제거하고 기존 기록을 IDE 키맵의 로컬 액션으로 연결한다. FR-019, FR-020.
- [x] T020 [US6] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에서 버튼 부재·탐색 액션 등록 및 기존 기록 보존을 검증한다. FR-019, FR-020.
- [x] T021 README.md·specs/037-trace-source-navigation/contracts/navigation.md·quickstart.md에 테이블 규칙·버튼 제거·단축키와 전체 SQL 회귀 결과를 기록한다. FR-018, SC-005.

T016 → T017 → T020 → T018 → T019 → T021 순서로 진행한다. 같은 분석·UI 파일을 변경하므로 순차 작업한다. 최소 제공 범위는 사용자 이야기 5·6 전체다. 구현 후 최신 명세·계획·작업과 코드를 수렴 점검한다.
