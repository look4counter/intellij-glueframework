# 작업 목록

## 1단계: 기반과 분석

- [x] T001 specs/036-merge-format-dml-trace/spec.md·plan.md·research.md·data-model.md를 작성하고 명확화 및 요구사항 대응을 분석한다. FR-001~FR-006.

## 2단계: MERGE 정렬

- [x] T002 [US1] src/test/kotlin/com/look4counter/SqlFormatterTest.kt에 정확한 예제·거친 들여쓰기·한 줄·멱등성 회귀를 작성한다. FR-001, FR-006.
- [x] T003 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormatter.kt에서 MERGE 헤더·USING·ON·SET 및 목록 정렬을 수정한다. FR-001.

## 3단계: 변경문 컬럼 추적

- [x] T004 [US2] src/test/kotlin/com/look4counter/SqlColumnTraceTest.kt에 UPDATE·INSERT·MERGE·DELETE·표현식·슬롯 격리·모호함 회귀를 작성한다. FR-002~FR-006.
- [x] T005 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlQueryParser.kt에 변경문 쓰기 정보와 INSERT 입력 쿼리 연결을 구현한다. FR-002, FR-003, FR-005.
- [x] T006 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlColumnTrace.kt에서 변경문 계보와 쓰기 대상·상수 입력을 기존 그래프에 연결하고 SELECT·조건 격리를 유지한다. FR-002~FR-005.

## 4단계: 통합과 마무리

- [x] T007 관련 포맷·컬럼·테이블·편집기 회귀를 실행하고 specs/036-merge-format-dml-trace/quickstart.md에 결과를 기록한다. FR-006, SC-001~SC-003.
- [x] T008 specs/036-merge-format-dml-trace의 명세·계획·작업과 코드를 speckit-converge로 대조하고 잔여 작업을 완료한다.

T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 순서로 실행한다. 두 이야기는 독립 검증할 수 있지만 공통 검증 환경을 순차 사용한다. 최소 구현은 사용자 MERGE 예제와 UPDATE·INSERT·MERGE의 컬럼 연결을 함께 제공하는 것이다.

- [x] T009 [US1] SqlFormattingTokens.kt·SqlFormatter.kt에서 포맷 출력의 대소문자 변환을 제거하고 SqlFormatterTest.kt·QuerySqlEditorTest.kt의 기존 대문자화 기대를 원문 보존 계약으로 갱신한다. FR-007. T007 전에 수행한다.

## 5단계: 수렴 보완

- [x] T010 [US2] SqlColumnTrace.kt에서 INSERT SELECT의 별표 출력 폭을 CTE·파생 테이블까지 확인하고 폭을 모르면 대상 슬롯을 연결하지 않도록 보완한다. SqlColumnTraceTest.kt에 별표 뒤의 식별자가 앞쪽 대상으로 잘못 연결되지 않는 회귀를 추가한다. FR-003의 부분 충족을 보완한다.

## 6단계: SELECT 추적 의도 정정

사용자 최신 명확화로 T004~T006·T010의 쓰기 대상 연결 범위는 대체한다.

- [x] T011 [US2] src/test/kotlin/com/look4counter/SqlColumnTraceTest.kt의 변경문 기대를 내부 SELECT·CTE·파생 출력만 표시하고 쓰기 대상·VALUES·DELETE는 제외하는 회귀로 갱신한다. FR-002~FR-006.
- [x] T012 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlColumnTrace.kt·SqlQueryParser.kt에서 불필요한 쓰기 계보와 메타데이터를 제거하고 변경문 내부 SELECT 및 외부 참조의 출처 선택을 유지한다. FR-002~FR-005.
- [x] T013 관련 SQL 분석·포맷·실제 편집기 회귀를 실행하고 README.md·specs/036-merge-format-dml-trace/data-model.md·quickstart.md를 최신 SELECT 전용 동작으로 갱신한다. FR-006, SC-001~SC-003.
- [x] T014 speckit-converge로 최신 명세·계획·작업과 코드를 대조하고 남은 작업을 완료한다.

T011 → T012 → T013 → T014 순서로 수행한다. 명확화와 계획·작업 분석에서 FR-002~FR-006이 모두 대응하며 미해결 질문은 없다.

## 7단계: SELECT와 같은 이름의 쓰기 대상 격리

- [x] T015 [US2] SqlColumnTrace.kt에서 INSERT 대상 목록과 SET 대입 좌변을 출처 선택에서 제외하고 SqlColumnTraceTest.kt에 CTE 출력과 이름이 같은 쓰기 대상 회귀를 추가한다. FR-002·FR-004의 부분 충족을 보완한다.

## 8단계: 소스와 선택한 참조 표시

사용자의 최신 예제로 T011~T015의 대상 제외·외부 참조 숨김 기대를 대체한다.

- [x] T016 [US2] src/test/kotlin/com/look4counter/SqlColumnTraceTest.kt에 사용자 MERGE의 2행/4행 소스 및 선택 참조, UPDATE·INSERT 대상 문맥·대입 반대편 격리·기존 SELECT 회귀를 작성한다. FR-002~FR-006, FR-008.
- [x] T017 [US2] SqlColumnTrace.kt에서 변경문 물리 소스 선언과 선택 참조를 표시하고 SET·INSERT 대상은 자기 소스로 해석하며 대입 반대편과 연결하지 않는다. FR-002~FR-005, FR-008.
- [x] T018 관련 회귀를 실행하고 README.md·data-model.md·quickstart.md를 최신 소스·자기 참조 규칙으로 갱신한다. FR-006, SC-001~SC-003.
- [x] T019 speckit-converge로 최신 요구와 명세·계획·작업 및 코드를 대조하고 잔여 작업을 완료한다.

T016 → T017 → T018 → T019 순서로 실행한다. 명확화 후 분석에서 최신 소스·자기 참조 요구는 모두 대응하며 미해결 질문은 없다.

## 9단계: 전체 참조와 SELECT 값 추적

- [x] T020 [US2] SqlColumnTraceTest.kt에 사용자 예제의 TEST·'B'·b.TEST에서 같은 전체 경로, a.deptno의 모든 참조, 표현식·숫자·파라미터 값 선택과 다른 소스·문장·주석 격리 회귀를 작성한다. FR-009, FR-010.
- [x] T021 [US2] SqlColumnTrace.kt에서 선언 토큰을 제외한 변경문 참조 전체를 수집하고 SELECT expression 토큰 선택을 출력 슬롯으로 연결한다. FR-002~FR-005, FR-008~FR-010.
- [x] T022 관련 SQL·실제 편집기 회귀를 실행하고 README.md·quickstart.md·data-model.md를 전체 경로와 값 선택 규칙으로 갱신한다. FR-006, SC-001~SC-003.
- [x] T023 speckit-converge로 최신 요구·계획·작업과 구현을 대조하고 남은 작업을 완료한다.

T020 → T021 → T022 → T023 순서로 수행한다. 명확화·계획·작업 분석에서 전체 경로와 값 선택 요구가 모두 대응하며 미해결 질문은 없다.

## 10단계: 일관된 등호 정렬과 SET 첫 줄

- [x] T024 [US3] src/test/kotlin/com/look4counter/SqlFormatterTest.kt에 조건절·대입문·INSERT SELECT·그룹 격리·멱등성 및 SET 첫 줄 회귀를 작성한다. FR-011~FR-014.
- [x] T025 [US3] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormatter.kt에서 절별 등호 그룹과 SET 첫 표현식 들여쓰기를 적용한다. FR-011, FR-012.
- [x] T026 [US3] queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/GlueQueryToolConfigurable.kt의 옵션 문구를 조건절·SET절로 갱신하고 저장 필드를 유지한다. FR-013.
- [x] T027 포매터·편집기 회귀를 실행하고 specs/036-merge-format-dml-trace/quickstart.md에 결과를 기록한다. FR-014, SC-004.

의존성: T024 → T025 → T026 → T027. 최소 제공 범위는 US3 전체다. T026은 T025와 다른 파일이므로 병렬 가능하지만 이번에는 순차 진행한다. 구현 후 수렴 점검으로 FR-011~FR-014와 SC-004를 대조한다.

추가 문서 작업: T026과 함께 README.md의 옵션 설명을 조건절·SET절 범위로 갱신한다. FR-013.

## 11단계: UPDATE 분기 키워드 끝 정렬

사용자 이야기 4(P1)는 일반 UPDATE와 MERGE 내부 UPDATE 모두 WHERE·AND·OR 끝을 UPDATE 끝에 맞추는 것이다.

- [x] T028 [US4] src/test/kotlin/com/look4counter/SqlFormatterTest.kt에 사용자 MERGE 및 일반 UPDATE의 정확한 키워드 열과 멱등성 회귀를 작성한다. FR-015, FR-016.
- [x] T029 [US4] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormatter.kt의 MERGE UPDATE 분기 정렬 폭을 6으로 보정하고 WHEN에서 복귀한다. FR-015.
- [x] T030 포매터·편집기 테스트를 실행하고 specs/036-merge-format-dml-trace/quickstart.md에 검증 결과를 기록한다. FR-016, SC-005.

T028 → T029 → T030 순서로 수행한다. 단일 포매터를 공유하므로 순차 진행하며 최소 제공 범위는 이야기 4 전체다. 이전 단계 완료 내용은 유지한다.

## 12단계: ON 조건 개수별 들여쓰기

- [x] T031 [US5] src/test/kotlin/com/look4counter/SqlFormatterTest.kt에 단일·복수 ON 조건, OR·BETWEEN·CASE·서브쿼리 및 멱등성 회귀를 작성한다. FR-017, FR-018.
- [x] T032 [US5] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormatter.kt에서 ON 최상위 논리 연결을 판별하고 첫 조건과 연결어 들여쓰기를 보정한다. FR-017.
- [x] T033 specs/036-merge-format-dml-trace/quickstart.md에 ON 결과와 포매터·편집기 검증 및 수렴 결과를 기록한다. FR-018.

T028 → T031 → T029 → T032 → T030 → T033 순서로 실행한다. 같은 포매터·테스트를 수정하므로 병렬 작업은 없다. 최소 제공 범위는 UPDATE와 ON 보정 모두다.

T033의 사용자 문서 범위에 README.md의 ON 조건 개수별 들여쓰기 설명 갱신을 포함한다. FR-017, FR-018.
