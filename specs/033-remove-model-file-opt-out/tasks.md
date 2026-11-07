# 실행 작업: 모델 파일 사용 해제 옵션 제거

## 1단계: 명세와 분석

- [x] T001 spec.md를 명세·명확화하고 품질 점검을 완료한다. FR-001~004.
- [x] T002 plan.md·tasks.md를 분석해 요구·구현·검증 대응을 확인한다. FR-001~004.

## 2단계: 사용자 이야기 1

독립 검증: 옵션이 없고 이전 true가 false로 정규화되며 다른 설정과 일반 모델 동작은 유지된다.

- [x] T003 ModelEditorSettingsTest.kt·ModelServiceDirectEditorTest.kt·EditorOpenThreadTest.kt의 폐기 모드 검증을 새 계약으로 갱신한다. FR-001~004.
- [x] T004 ModelEditorSettings.kt·GlueModelEditorConfigurable.kt에서 UI를 제거하고 상태를 false로 고정한다. FR-001~002.

## 3단계: 검증과 수렴

- [x] T005 README.md·quickstart.md·기존 모델 명세에 변경을 기록하고 집중·전체 회귀·ZIP·구조 검사를 완료한다. FR-003~004·SC-001~003.
- [x] T006 명세·계획·작업과 구현을 speckit-converge로 대조해 남은 작업을 완료한다.

## 의존성과 실행 전략

T001→T002→T003→T004→T005→T006 순서로 진행한다. 모드 전용 UI 테스트 교체는 승인된 기능 폐기에 따른 계약 변경이며 내부 어댑터와 일반 모델 기능 검증을 삭제하지 않는다.

## 구현 전 분석 결과

요구사항 4개와 성공 기준 3개가 실행 작업에 대응한다. UI 삭제만으로 과거 true가 남는 문제를 상태 정규화로 해결하며 모호함·충돌·누락이 없다. 품질 점검을 통과했다. 헌장은 미작성 템플릿이며 확장 훅은 없다.
