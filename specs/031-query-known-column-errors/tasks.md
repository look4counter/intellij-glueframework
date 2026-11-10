# 실행 작업: 확정 컬럼 오류와 주석 색상

## 1단계: 명세와 분석

- [x] T001 specs/031-query-known-column-errors/spec.md를 명세·명확화하고 품질 점검을 완료한다. FR-001~008.
- [x] T002 specs/031-query-known-column-errors/plan.md·tasks.md를 분석해 요구·계획·검증 대응을 확인한다. FR-001~008.

## 2단계: 사용자 이야기 1·2

독립 검증: 확정 출력의 잘못된 컬럼만 구역별로 검출하고 불확정 관계·이름·범위에는 오탐이 없다.

- [x] T003 SqlColumnValidatorTest.kt에 확정/불확정·각 구역·DML·범위·별칭·자료형·미완성 회귀를 추가한다. FR-001~004·006~007.
- [x] T004 SqlQueryParser.kt에 검증 전용 DML 범위·관계를 추가한다. FR-002·004.
- [x] T005 SqlColumnValidator.kt에 확정 스키마와 부재 진단을 구현한다. FR-001~004·006.

## 3단계: 사용자 이야기 3

독립 검증: 실제 편집기에서 오류가 표시·갱신되고 문서를 바꾸지 않으며 주석의 두 테마 색상이 녹색이다.

- [x] T006 SqlColumnErrorHighlighting.kt·QuerySqlEditor.kt에 오류 표시·지연 분석·수명 보호를 연결한다. FR-005~006.
- [x] T007 SqlSyntaxHighlighter.kt·QuerySqlEditorTest.kt에 녹색 주석과 실제 오류 표시·수정·문서 전환 검증을 추가한다. FR-005·007~008.

## 4단계: 검증과 수렴

- [x] T008 README.md·quickstart.md를 갱신하고 집중·전체 회귀·ZIP·구조 검사를 완료한다. FR-007·SC-001~003.
- [x] T009 명세·계획·작업과 구현을 speckit-converge로 대조해 남은 작업을 완료한다.

## 의존성과 실행 전략

T001→T002→T003→T004→T005→T006→T007→T008→T009 순서로 진행한다. 검증기와 UI 검증은 별도 파일이지만 순차 실행한다. 확정성 분석을 먼저 완성한 뒤 오류 표시와 색상을 연결한다.

## 구현 전 분석 결과

요구사항 8개와 성공 기준 3개가 실행 작업에 대응한다. SELECT에 제한하지 않는 후속 지시와 녹색 주석 추가 요청을 반영했고 중요한 모호함·충돌·누락이 없다. 품질 점검은 통과다. 헌장은 미작성 템플릿으로 검사를 생략했고 확장 훅은 없다.

## 5단계: NULL 오탐 수정

- [x] T010 [US4] src/test/kotlin/com/look4counter/SqlColumnValidatorTest.kt에 NULL 리터럴의 직접·별칭·조건·DML 및 한정/인용 컬럼 검증 회귀를 작성한다. FR-009~FR-011.
- [x] T011 [US4] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 실제 NULL 오탐 제거와 누락 컬럼 표시·문서 보존 회귀를 작성한다. FR-010, FR-011.
- [x] T012 [US4] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlColumnValidator.kt에서 비인용 단독 NULL을 문법 보호 목록에 포함한다. FR-009, FR-010.
- [x] T013 specs/031-query-known-column-errors/contracts/editor.md에 NULL 보호 계약을 기록하고 quickstart.md에 집중·전체 SQL 회귀 및 수렴 결과를 기록한다. FR-011, SC-004.

T010 → T011 → T012 → T013 순서로 진행한다. 단위·UI 검증 파일은 독립적이지만 순차 수행한다. 최소 제공 범위는 이야기 4 전체다. 구현 후 최신 요구와 명세·계획·작업을 대조한다.
