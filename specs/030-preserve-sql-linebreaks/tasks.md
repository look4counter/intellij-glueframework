# 실행 작업: SQL 사용자 줄바꿈 보존

## 1단계: 명세와 기반

- [x] T001 specs/030-preserve-sql-linebreaks/spec.md를 명세·명확화하고 품질 점검을 완료한다. FR-001~006.
- [x] T002 specs/030-preserve-sql-linebreaks/plan.md·tasks.md를 분석해 요구·계획·검증 대응을 확인한다. FR-001~006.

## 2단계: 사용자 이야기 1

독립 검증: 한 줄 입력은 한 줄이고 기존 줄바꿈의 토큰 경계·빈 줄·복합 키워드·주석·리터럴은 유지된다.

- [x] T003 src/test/kotlin/com/look4counter/SqlFormatterTest.kt에 원본 줄바꿈 보존 검증을 추가하고 기존 자동 줄바꿈 기대값을 갱신한다. FR-001~004·006.
- [x] T004 queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormatter.kt의 Writer·복합 절·재귀 출력·선택 경계 처리를 원본 줄바꿈 기준으로 변경한다. FR-001~004.

## 3단계: 사용자 이야기 2

독립 검증: 저장 옵션·전체/선택 명령·Source·Undo/Redo가 새 행 구성 계약을 만족한다.

- [x] T005 src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt의 전체·선택·옵션·Undo/Redo 검증을 갱신한다. FR-004~006.

## 4단계: 검증과 수렴

- [x] T006 README.md와 quickstart.md를 갱신하고 집중·전체 회귀·ZIP·구조 검증을 완료한다. FR-006·SC-001~003.
- [x] T007 명세·계획·작업과 현재 구현을 speckit-converge로 대조하고 남은 작업을 완료한다.

## 의존성과 실행 전략

T001→T002→T003→T004→T005→T006→T007 순서로 실행한다. 분석기와 편집기 검증은 별도 파일이지만 순차 진행한다. 먼저 행 보존을 구현하고 기존 편집 명령에 대한 통합 검증으로 완성한다.

## 구현 전 분석 결과

요구사항 6개와 성공 기준 3개가 모든 실행 작업에 대응한다. 기존 자동 줄바꿈과 충돌하는 기대값을 명시적으로 변경하며 미해결 모호함·누락은 없다. 품질 점검 6개가 통과했다. 헌장은 미작성 템플릿으로 검사를 생략했고 확장 훅은 없다.

## 5단계: 줄바꿈 대체 공백 취소

- [x] T008 SqlFormatterTest.kt·QuerySqlEditorTest.kt의 단일 행 공백 기대값을 일반 구문 규칙으로 갱신하고 콤마·괄호·키워드 경계 회귀를 추가한다. FR-002·005~006.
- [x] T009 SqlFormatter.kt에서 같은 행의 줄바꿈 요청을 무시하고 일반 토큰 구분 공백을 분리한다. FR-001~004.
- [x] T010 README.md·quickstart.md를 갱신하고 집중·전체 회귀·ZIP·구조 검사와 수렴 검토를 완료한다. FR-006·SC-001~003.

후속 명세·명확화·계획·작업 분석 결과: 사용자의 공백 대체 취소를 FR-002에 반영했고 세 작업이 전체 요구와 검증에 대응한다. 일반 구문 공백과 자동 줄바꿈 대체를 구분해 모호함·충돌·누락이 없다. 품질 점검은 통과다.

## 6단계: 인라인 서브쿼리 정렬 보완

- [x] T011 SqlFormatterTest.kt·QuerySqlEditorTest.kt에 사용자 예제·중첩·별도 줄 SELECT·정렬 옵션·반복 실행 검증을 추가한다. FR-007.
- [x] T012 SqlFormatter.kt의 후속 절 정렬 기준을 실제 SELECT 시작 열로 갱신한다. FR-001·004·007.
- [x] T013 quickstart.md·README.md를 갱신하고 전체 회귀·ZIP·구조 검사와 수렴 검토를 완료한다. FR-006~007·SC-001~003.

후속 분석 결과: FR-007의 실제 열 기준·중첩 격리·줄바꿈 보존이 T011~T013에 대응한다. 품질 점검은 통과이며 중요한 모호함·충돌·누락은 없다.
