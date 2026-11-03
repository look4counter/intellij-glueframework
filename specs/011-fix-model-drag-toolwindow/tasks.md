# 작업 목록: 모델 드래그와 Tool Window 충돌

## 단계 1: 준비

- [x] T001 specs/011-fix-model-drag-toolwindow/research.md의 실제 콜백 경로·stamp·등록 충돌 원인을 확인한다.

## 단계 2: 기반 확인

- [x] T002 specs/011-fix-model-drag-toolwindow/의 명세·계획·작업과 품질 점검을 대조한다.

## 단계 3: 사용자 이야기 1

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 잠금 없는 실제 Child·Wire·크기 조절·원문 변경 취소 검증을 추가하고 수정 전 오류를 확인한다. FR-001~FR-003.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에서 editNodes/editWire에 안전 문맥 가드와 고정 stamp를 적용한다. FR-001, FR-002.
- [x] T005 [US1] src/test/kotlin/com/look4counter/ModelDiagramTest.kt와 ModelDiagramPropertiesTest.kt를 실행한다. FR-003, SC-001, SC-002.

## 단계 4: 사용자 이야기 2

- [x] T006 [US2] src/main/resources/META-INF/plugin.xml과 messages/MyMessageBundle.properties의 Tool Window ID·표시 이름을 고유하게 맞춘다. FR-004.
- [x] T007 [US2] src/test/kotlin/com/look4counter/ToolWindowRegistrationTest.kt에서 공통 ID와 공존 및 팩토리·콘텐츠·표시 이름을 확인한다. FR-004, FR-005.

## 단계 5: 마무리

- [x] T008 specs/011-fix-model-drag-toolwindow/quickstart.md에 결과와 한계를 기록하고 ZIP·문서·구현 일치를 확인한다. FR-005.

## 의존성과 전략

T001 → T002 후 드래그는 T003 → T004 → T005, Tool Window는 T006 → T007 순서로 수행하고 T008로 마무리한다. 두 이야기는 드래그와 Tool Window 등록으로 독립 검증한다. 독립 원인 조사는 계획 단계의 연구 에이전트가 수행했다.
