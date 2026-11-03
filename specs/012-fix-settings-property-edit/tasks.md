# 작업 목록: 설정 속성 편집 유지

## 1단계: 준비

- [x] T001 specs/012-fix-settings-property-edit/research.md에 원인과 기존 확정 계약을 조사한다.

## 2단계: 기반 설계

- [x] T002 specs/012-fix-settings-property-edit/contracts/settings.md에 순수 조회·확정 계약을 정의한다.

## 3단계: 사용자 이야기 1

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelEditorSettingsTest.kt에 반복 조회·키와 값·적용·초기화·전환 회귀를 추가하고 실패를 확인한다. FR-001~004, SC-001~002.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramDialogs.kt에 편집 유지 스냅샷을 추가한다. FR-001~002.
- [x] T005 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/GlueModelEditorConfigurable.kt에서 조회용 복제본을 비교하도록 변경한다. FR-001~004.

## 4단계: 검증과 마무리

- [x] T006 specs/012-fix-settings-property-edit/quickstart.md에 설정·속성 테스트 결과와 수렴 점검 결과를 기록한다. SC-001~002.

## 의존성과 구현 전략

T001 → T002 → T003 → T004 → T005 → T006 순서다. 단일 사용자 이야기 전체가 최소 제공 범위이며 같은 경로의 변경과 검증이므로 순차 수행한다. 구현 완료 후 두 테스트 클래스 실행은 함께 요청할 수 있다.
