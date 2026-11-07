# 실행 작업: SQL 컬럼 TRACE

## 1단계: 명세와 기반

- [x] T001 specs/028-query-column-trace/spec.md를 명세·명확화하고 품질 점검을 통과한다. FR-001~007.
- [x] T002 specs/028-query-column-trace/plan.md·tasks.md를 분석해 일관성과 요구사항 대응을 확인한다. FR-001~007.

## 2단계: 사용자 이야기 1

독립 검증: 사용자 예제의 마지막 SS에서 원본 순서 2·6·9행과 정확한 내용을 반환한다.

- [x] T003 [US1] src/test/kotlin/com/look4counter/SqlColumnTraceTest.kt에 CTE·별표·별칭·명시 목록·표현식·재귀·모호함·격리 검증을 작성한다. FR-001~002·006~007.
- [x] T004 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlColumnTrace.kt에 위치를 보존하는 추적기를 구현한다. FR-001~002·006.

## 3단계: 사용자 이야기 2

독립 검증: 목록 클릭·Enter로 이동하며 SQL을 유지하고 변경·전환 후 오래된 항목을 버린다.

- [x] T005 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryColumnTracePanel.kt와 QueryDesignEditor.kt에 패널·비동기 갱신·탐색·수명 보호를 구현한다. FR-003~006.
- [x] T006 [US2] src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 실제 목록·이동·수정·쿼리 전환 검증을 추가한다. FR-003~005·007.

## 4단계: 검증과 문서

- [x] T007 README.md와 specs/028-query-column-trace/quickstart.md를 갱신하고 전체 회귀·ZIP·구조 검사를 수행한다. FR-007·SC-001~003.
- [x] T008 specs/028-query-column-trace의 명세·계획·작업과 구현을 수렴 검토하고 남은 작업을 완료한다.

## 의존성과 실행 전략

T001→T002→T003→T004→T005→T006→T007→T008 순서로 실행한다. 분석기와 UI 테스트는 별도 파일이지만 이번에는 순차 실행한다. 최소 완성 단위는 US1이며 US2로 실제 사용자 기능을 완성한다.

## 구현 전 분석 결과

기능 요구사항 7개와 성공 기준 3개가 모두 T003~T008에 연결된다. 미해결 질문·중요한 모호함·충돌·누락 작업은 0개다. 품질 점검 6개가 통과했고 헌장은 미작성 템플릿이므로 규범 검사를 생략했다. 확장 훅 설정은 없다.

## 5단계: 컬럼 전체 경로와 제목 제거

- [x] T009 src/test/kotlin/com/look4counter/SqlColumnTraceTest.kt에 원본·중간·최종 위치의 전체 경로, 이름 변경·분기·별표 슬롯 격리 검증을 추가한다. FR-008.
- [x] T010 queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlColumnTrace.kt에 범위·출력 슬롯을 구분하는 양방향 연결 탐색과 원본 우선 표시를 구현한다. FR-001~002·006·008.
- [x] T011 QueryColumnTracePanel.kt에서 제목 라벨을 제거하고 QuerySqlEditorTest.kt에 원본에서 최종 사용처까지의 목록과 기존 이동 동작을 확인한다. FR-003~005·008.
- [x] T012 README.md·quickstart.md를 갱신하고 전체 회귀·ZIP·구조 검증과 수렴 검토를 수행한다. FR-007~008·SC-001~003.

## 6단계: 컬럼 출처와 출력 전달에 집중

- [x] T013 SqlColumnTraceTest.kt에 제외 절·조건 서브쿼리·SELECT 표현식 입력 검증을 추가하고 기존 ON·ORDER 기대값을 갱신한다. FR-009.
- [x] T014 SqlColumnTrace.kt의 그래프 참조·하위 범위·표시를 SELECT 출력 전달로 제한하고 제외 절의 커서는 표시 없는 대상 해석으로 유지한다. FR-001·003·008~009.
- [x] T015 QuerySqlEditorTest.kt에 SELECT 목록·제외 절·복귀 시 갱신 검증을 추가하고 README.md·quickstart.md 갱신, 전체 회귀·ZIP·구조 검사와 수렴 검토를 완료한다. FR-005·007·009·SC-001~003.

## 7단계: 빈 상태 안내 단순화

- [x] T016 QueryColumnTracePanel.kt의 하단 상태 라벨을 제거하고 목록 내부 고정 안내와 세로 가운데 배치를 적용한다. FR-010.
- [x] T017 QuerySqlEditorTest.kt에서 초기·분석 중·출처 불명·경로 표시/복귀와 라벨 부재·가운데 배치를 확인한다. FR-005·010.
- [x] T018 README.md·quickstart.md를 갱신하고 관련 회귀·ZIP·구조 검사와 수렴 검토를 완료한다. FR-007·010·SC-002~003.

## 8단계: 항목 패딩 통일

- [x] T019 QueryColumnTracePanel.kt의 항목 렌더러에 쿼리 목록과 동일한 좌우 8 논리 픽셀 패딩을 적용한다. FR-011.
- [x] T020 기존 QuerySqlEditorTest 회귀·ZIP·구조 검사를 수행하고 quickstart.md 결과 기록과 수렴 검토를 완료한다. FR-004~005·011·SC-002~003.

후속 명세·명확화·계획·작업 분석 결과: FR-011의 항목 여백·배율·선택/포커스·이동 보존이 두 작업에 대응한다. 중요한 모호함·충돌·누락은 없으며 기존 품질 점검은 통과다.

후속 명세·명확화·계획·작업 분석 결과: FR-010의 라벨 제거·고정 문구·세로 가운데·동작 보존이 세 작업에 대응한다. 미해결 모호함·충돌·누락이 없으며 품질 점검은 통과다.

후속 명세·명확화·계획·작업 분석 결과: FR-009의 출력 전달·제외 절·조건 하위 범위·안내·갱신이 세 작업과 검증에 대응한다. 기존 전체 경로는 출력 전달 범위로 구체화했으며 중요한 모호함·충돌·누락이 없다. 품질 점검은 통과다.

명세·명확화·계획·작업 분석 결과: 후속 요구 FR-008과 FR-003의 변경이 네 작업에 모두 대응한다. 커서까지만 표시하던 기존 정의는 전체 경로로 갱신했으며 미해결 모호함·충돌·누락은 없다. 기존 품질 점검 6개는 통과다.
