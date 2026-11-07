# 실행 작업: SQL 문맥 자동완성

## 1단계: 준비

- [x] T001 specs/026-contextual-sql-completion/spec.md와 설계·명확화·품질 점검을 검토한다.

## 2단계: 기반

- [x] T002 specs/026-contextual-sql-completion/plan.md·tasks.md를 speckit-analyze로 검토해 요구사항과 작업의 일관성을 확인한다.
- [x] T003 src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 FROM 후보 구분과 별칭 후보·삽입 통합 검증을 먼저 추가하고 기존 구현의 실패를 확인한다. FR-001~003, FR-008.

## 3단계: 사용자 이야기 1

독립 검증: 구역별 후보 종류와 커서 뒤 FROM 해석, 불완전 입력 복구가 통과한다.

- [x] T004 [US1] src/test/kotlin/com/look4counter/SqlCompletionAnalyzerTest.kt에 구역·접두·문장·불완전 입력·보호·제한 검증을 추가한다. FR-001~002, FR-007~009.
- [x] T005 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlQueryParser.kt에 어휘·괄호·문장·블록·구역과 관계 구조를 구현한다. FR-001~002, FR-007.
- [x] T006 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlCompletionAnalyzer.kt와 SqlKeywordCompletionProvider.kt에 문맥별 후보·기존 키워드·접두·보호·복구를 연결한다. FR-001~003, FR-007~008.

## 4단계: 사용자 이야기 2

독립 검증: 별칭·스키마·중복 컬럼·상관 범위와 실제 후보 삽입이 통과한다.

- [x] T007 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlQueryScope.kt와 src/test/kotlin/com/look4counter/SqlCompletionAnalyzerTest.kt에 별칭·스키마·가림·상관·형제 격리·중복 컬럼 해석과 검증을 구현한다. FR-002~004, FR-006~007.

## 5단계: 사용자 이야기 3

독립 검증: CTE 출력 이름·앞선 참조·중첩·재귀 안전성과 파생 테이블 후보가 통과한다.

- [x] T008 [US3] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlQueryParser.kt·SqlQueryScope.kt에 CTE와 파생 테이블의 명시 컬럼·출력 별칭·별표·범위를 구현하고 src/test/kotlin/com/look4counter/SqlCompletionAnalyzerTest.kt에서 검증한다. FR-004~005, FR-007.
- [x] T009 [US3] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 CTE·중복 컬럼 후보 선택과 Source 반영·Undo/Redo를 추가하고 기존 메타데이터 기대값을 새 구역 계약에 맞춘다. FR-005~006, FR-008~009.

## 6단계: 검증과 마무리

- [x] T010 README.md와 specs/026-contextual-sql-completion/quickstart.md에 범위·제한·검증 결과를 기록하고 전체 테스트·ZIP·구조 검증을 수행한다. FR-009, SC-001~003.
- [x] T011 specs/026-contextual-sql-completion/spec.md·plan.md·tasks.md와 구현을 speckit-converge로 대조하고 남은 작업을 완료한다.

## 의존성과 실행 전략

T001→T002→T003→T004→T005→T006→T007→T008→T009→T010→T011 순서로 진행한다. 최소 범위는 US1이며 US2·US3를 같은 파싱 모델 위에 차례로 완성한다. 순수 분석과 실제 편집기 검증은 서로 다른 파일이지만 이번 작업은 순차 실행한다. 테스트를 삭제하거나 무시하지 않는다.

## 7단계: 파생 테이블 경계 회귀

- [x] T012 SqlCompletionAnalyzerTest.kt·QuerySqlEditorTest.kt에 사용자 예제 A/B 후보 분리·삽입 검증을 추가한다. FR-010.
- [x] T013 SqlQueryParser.kt에서 인접 SELECT/WITH 괄호와 별칭 컬럼 목록을 구분한다. FR-010.
- [x] T014 전체 테스트·ZIP·구조·문서 갱신과 수렴 검증을 수행한다. FR-009~010.

후속 명세·명확화·계획·작업 분석에서 세 작업이 요구를 모두 포함하며 기존 명시 목록·범위 보호와 충돌이 없다. 기존 품질 점검은 통과한다.

## 8단계: 두 번째 SELECT 항목 확인

- [x] T015 src/test/kotlin/com/look4counter/SqlCompletionAnalyzerTest.kt와 QuerySqlEditorTest.kt에 사용자 전체 SQL의 B. 후보와 삽입 검증을 추가한다. FR-011.
- [x] T016 실패하면 해당 분석·완성 경로를 수정하고 통과하면 기존 구현의 충족 사실을 확인한다. FR-011.
- [x] T017 전체 회귀·설치 ZIP·구조 검증, quickstart.md 결과 기록과 수렴 검토를 완료한다. FR-009·FR-011.

분석 결과: FR-011의 후보·오프셋·삽입·원문 보존이 T015~T017과 연결된다. 기존 FR-001~010과 충돌하거나 미해결된 사항은 없다. 구현에 앞선 품질 점검 6개는 모두 통과다.

## 9단계: 종류·문맥 정렬

- [x] T018 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlKeywordCompletionProvider.kt에 종류 우선순위와 이름 비교기를 적용하고 컬럼 문맥·테이블 문맥의 최상위 그룹과 병합 종류의 최소 순위를 처리한다. FR-012~FR-014.
- [x] T019 [US1] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에서 실제 팝업의 컬럼 우선·CTE/테이블 혼합·일반 후보 종류·한정자 ABC순을 검증하고 SqlCompletionAnalyzerTest.kt의 기존 문맥 회귀를 실행한다. FR-012~FR-014.
- [x] T020 설치 ZIP·플러그인 구조 검증과 specs/026-contextual-sql-completion/quickstart.md 결과 기록 및 수렴을 완료한다. FR-009, FR-012~FR-014.

T018 → T019 → T020 순서로 수행한다. 후보 생성과 저장 모델은 변경하지 않는다.
