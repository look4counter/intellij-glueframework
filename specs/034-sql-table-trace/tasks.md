# 실행 작업: SQL 테이블 추적

## 1단계: 준비

- [x] T001 specs/034-sql-table-trace/spec.md를 명세·명확화하고 checklists/requirements.md의 품질 점검을 완료한다. FR-001~006.
- [x] T002 specs/034-sql-table-trace/plan.md·research.md·data-model.md·contracts/trace-ui.md·quickstart.md를 작성하고 분석 준비를 완료한다. FR-001~006.

## 2단계: 공통 기반

- [x] T003 queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlQueryParser.kt에 CTE 선언·소스 경로 및 별칭 위치를 보존한다. FR-001·003.

## 3단계: 사용자 이야기 1

독립 검증: CTE 연쇄·분기·파생 테이블의 테이블 위치를 선택하면 원본부터 전체 참조 분기를 반환한다.

- [x] T004 [US1] src/test/kotlin/com/look4counter/SqlTableTraceTest.kt에 연쇄·분기·JOIN 입력 격리·파생·UNION·위치 검증을 작성한다. FR-001~004·SC-001.
- [x] T005 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlTableTrace.kt에 선택·관계 그래프·원본 및 소비자 순회·행 생성 기능을 구현한다. FR-001~005.

## 4단계: 사용자 이야기 2

독립 검증: A.COL의 A와 COL을 각각 선택하면 테이블과 컬럼 경로가 분리되며 실제 목록에서 정확히 이동한다.

- [x] T006 [US2] src/test/kotlin/com/look4counter/SqlTableTraceTest.kt에 문장·별칭·인용 이름·이름 가림·재귀·주석·문자열·한도·불완전 입력 검증을 추가한다. FR-003·005.
- [x] T007 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlTrace.kt에 테이블 우선 및 기존 컬럼 대체 진입점을 구현한다. FR-001·005.
- [x] T008 [US2] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 테이블 탐색·컬럼 전환·무수정·갱신 검증을 추가한다. queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt에 통합 진입점과 안내 문구를 적용한다. FR-004~006·SC-002.

## 5단계: 검증과 마무리

- [x] T009 README.md와 specs/034-sql-table-trace/quickstart.md에 사용법과 집중·전체 회귀 결과를 기록하고 build/distributions/intellij-glueframework-1.0.4.zip 및 구조 검사를 완료한다. SC-001~003.
- [x] T010 specs/034-sql-table-trace/spec.md·plan.md·tasks.md와 구현을 speckit-converge로 대조하고 남은 작업까지 완료한다.

## 의존성과 실행 전략

T001→T002→분석→T003→T004→T005→T006→T007→T008→T009→T010 순서로 진행한다. 사용자 이야기 2는 이야기 1의 테이블 분석기를 재사용한다. T004와 T006의 서로 독립된 시나리오 설계는 병렬화 가능하지만 동일 테스트 파일을 사용하므로 실제 수정은 순차 수행한다. 하위 에이전트는 필요하지 않다. 최소 동작 단위는 이야기 1의 분석이며 이번 요청은 두 이야기를 함께 완료한다.

## 6단계: 수렴

- [x] T011 queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlTableTrace.kt에서 PIVOT·UNPIVOT·MODEL·MATCH_RECOGNIZE 구문을 별칭 선택에서 제외하고 src/test/kotlin/com/look4counter/SqlTableTraceTest.kt에 인용 별칭과 구문 키워드 구분 회귀를 추가한 뒤 최종 전체 검증과 문서 기록을 갱신한다. FR-005의 부분 구현을 보완한다.

## 7단계: 선택 동기화 및 마우스 누름

- [x] T012 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlColumnTrace.kt와 SqlTableTrace.kt에 선택 단계의 selectedOffset을 추가하고 src/test/kotlin/com/look4counter/SqlColumnTraceTest.kt와 SqlTableTraceTest.kt에서 출력·참조·한정자의 위치를 검증한다. FR-007.
- [x] T013 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt에서 결과 위치와 목록 선택·스크롤을 동기화하고 마우스 누름과 Enter 이동 시 선택 행을 유지한다. FR-007, FR-008.
- [x] T014 [US2] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에서 커서·목록 선택, 한정자 대응, 미세 마우스 이동과 무클릭, 우클릭·빈 영역·오래된 결과 방어 및 SQL·Source 비변경을 검증한다. FR-004, FR-007, FR-008.
- [x] T015 관련 회귀와 buildPlugin·verifyPluginStructure를 수행하고 specs/034-sql-table-trace/quickstart.md에 결과를 기록한 뒤 명세·계획·작업과 구현의 수렴을 확인한다.

T012 → T013 → T014 → T015 순서로 진행한다. 기존 경로·순서·표시 행은 바꾸지 않고 선택 위치만 추가한다.

## 8단계: 실제 경로를 보존하는 표준 트리

- [x] T016 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlTraceGraph.kt에 객체 식별·직접 관계 축약·루트·선택 ID를 가진 그래프를 구현하고 SqlTraceGraphTest.kt에서 연결·분기·재합류·순환 및 한도를 검증한다. FR-010, FR-013.
- [x] T017 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlColumnTrace.kt와 SqlTableTrace.kt에서 표시 이름과 실제 관계 그래프를 반환하고 기존 분석기 테스트에 동일 줄·별표·분기·격리 검증을 추가한다. FR-010, FR-011.
- [x] T018 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt를 표준 Tree로 바꾸고 경로별 공유 노드·순환·한도 표시 및 선택 동기화·안전한 누름/Enter·기본 접기/펼치기를 연결한다. FR-009~FR-013.
- [x] T019 [US2] 같은 패널에서 문서·stamp·경로 키로 펼침 상태를 보존하고 src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt의 기존 목록 검증을 트리로 갱신해 구조·화살표·키보드·펼침 유지·선택·SQL/Source 비변경을 검증한다. FR-007~FR-013.
- [x] T020 관련 분석·실제 편집기 회귀와 buildPlugin·verifyPluginStructure를 수행하고 specs/034-sql-table-trace/quickstart.md에 결과를 기록한 뒤 수렴 점검을 수행한다. SC-001~SC-004.

T016 → T017 → T018 → T019 → T020 순서로 실행한다. 이전 작업은 완료 상태를 유지하고 트리 전환 시점부터 UI 계약을 적용한다.
