# 실행 작업: SQL 포매팅 정렬 옵션

## 1단계: 명세와 분석

- [x] T001 specs/029-sql-format-alignment/spec.md를 명세·명확화하고 품질 점검을 완료한다. FR-001~007.
- [x] T002 specs/029-sql-format-alignment/plan.md·tasks.md를 분석해 요구·계획·작업 일관성을 확인한다. FR-001~007.

## 2단계: 사용자 이야기 1

독립 검증: 기본값·개별 옵션·Apply·Reset·직렬화·검증 실패가 기존 DataSource 동작을 유지한다.

- [x] T003 src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt에 포매팅 설정의 수명·저장·복원·독립성 검증을 추가한다. FR-001~002·007.
- [x] T004 queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/QueryFormattingSettings.kt와 GlueQueryEditorConfigurable.kt에 저장 서비스·체크박스를 구현한다. FR-001~002.

## 3단계: 사용자 이야기 2

독립 검증: 두 옵션의 개별·동시 정렬과 중첩·주석·CAST·다른 연산자 보호, 반복 실행을 확인한다.

- [x] T005 src/test/kotlin/com/look4counter/SqlFormatterTest.kt에 옵션별 기대 결과와 구역·문장 격리·들여쓰기·보호 회귀를 추가한다. FR-003~005·007.
- [x] T006 queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormatter.kt에 토큰별 정렬량 수집과 재렌더링을 구현한다. FR-003~005.
- [x] T007 QueryDesignEditor.kt와 QuerySqlEditorTest.kt에 저장 옵션을 사용한 전체·선택 포매팅 및 Undo/Redo 검증을 연결한다. FR-006~007.

## 4단계: 마무리

- [x] T008 README.md·quickstart.md를 갱신하고 집중·전체 회귀·ZIP·구조 검증을 완료한다. FR-007·SC-001~003.
- [x] T009 명세·계획·작업과 현재 구현을 speckit-converge로 대조하고 남은 작업을 완료한다.

## 5단계: IntelliJ 설정 화면 외형 보완

- [x] T010 GlueQueryEditorConfigurable.kt의 사각 테두리를 기본 TitledSeparator와 들여쓴 체크박스 배치로 교체한다. FR-008.
- [x] T011 기존 DataSourceSettingsTest 회귀·ZIP·구조 검증과 quickstart.md 결과 기록·수렴 검토를 완료한다. FR-001~002·008·SC-001.

후속 명세·명확화·계획·작업 분석 결과: FR-008의 사각 테두리 제거·기본 컴포넌트·배율·동작 보존이 두 작업에 대응한다. 중요한 모호함·충돌·누락이 없고 기존 품질 점검은 통과다.

## 의존성과 실행 전략

T001→T002→T003→T004→T005→T006→T007→T008→T009 순서로 진행한다. 설정과 포매터 검증은 다른 파일이지만 이번에는 순차 실행한다. US1의 설정 저장을 먼저 완성한 뒤 US2로 실제 포매팅을 연결한다.

## 구현 전 분석 결과

기능 요구사항 7개와 성공 기준 3개가 T003~T009에 모두 대응한다. 미해결 모호함·충돌·누락은 0개다. 품질 점검 6개를 통과했다. 헌장은 미작성 템플릿이므로 규범 검사를 생략했고 확장 훅은 없다.
