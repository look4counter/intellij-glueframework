# 작업 목록: Resize 보정과 Magnetic 원복

## 1단계: 준비

- [x] T001 specs/005-fix-state-wire-center/spec.md와 설계·품질 점검 확인

## 2단계: 기반

- [x] T002 modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt와 ModelDesignEditorProvider.kt의 미리보기·저장 경로 확인

## 3단계: 사용자 이야기 1 및 2

- [x] T003 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramGeometry.kt의 기존 상태 외곽 연결을 회귀 검증으로 보존 (FR-001)
- [x] T004 [US2] src/test/kotlin/com/look4counter/ModelEditorFeaturesTest.kt의 후보 검사와 modeleditor/src/main/kotlin/com/look4counter/modeleditor/WireSnap.kt를 최근접 후보로 원복 (FR-002)

## 4단계: 사용자 이야기 3

- [x] T005 [US3] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 Resize의 상대 좌표 보정 및 절대 꺾임점 보존·저장·실행 취소 검증 추가 (FR-003~005)
- [x] T006 [US3] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt에서 Resize 중심 변화량 보정 (FR-003)
- [x] T007 [US3] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에서 보정된 꺾임점 차원 저장 (FR-004~005)

## 5단계: 사용자 이야기 4

- [x] T008 [US4] modeleditor/src/main/kotlin/com/look4counter/modeleditor/WireSnap.kt·ModelDiagramCanvas.kt·ChildCreationToolbar.kt·ModelDesignEditorProvider.kt와 관련 테스트의 용어를 Snap으로 변경 (FR-006)
- [x] T009 [US4] src/test/kotlin/com/look4counter/ModelDiagramAlignmentTest.kt에 Snap 상태별 이동·Resize·Alt·드래그 중 끄기 검증 추가 (FR-007~008)
- [x] T010 [US4] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt의 Snap 꺼짐 정렬 우회와 보조선 정리 구현 (FR-007~008)

## 6단계: 검증

- [x] T011 관련 테스트 결과를 specs/005-fix-state-wire-center/quickstart.md에 기록하고 수렴 점검

## 의존성과 전략

T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 → T009 → T010 → T011 순서로 실행한다.
US1은 기존 외곽 검증, US2는 최근접 후보, US3은 절대 꺾임점과 XML 저장을 각각 독립 검증한다.
서로 다른 파일의 원복과 보정은 병렬 가능하나 이번에는 순차 실행한다. 네 이야기 모두 이번 제공 범위다.




## 7단계: 수렴 점검

FR-003·FR-005의 꺾임점 없는 경계 사례를 점검했다. 저장된 선호 접점이 없는 Activity 간 직선은 기존 중심을 기본값으로 사용하여 Resize 후 연결 위치가 달라질 수 있다. 심각도는 높음이며 기존 접점이 새 경계 안에서 유효하면 위치를 유지해야 한다.

- [x] T012 [US3] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt에서 저장 접점 없는 직선의 Resize 전 접점을 유지하고 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 양 끝 노드·좌우 조절 회귀 추가 (FR-003·FR-005 부분 충족 보완)
- [x] T013 src/test/kotlin/com/look4counter/ModelDiagramTest.kt 등 관련 검증을 다시 실행하고 specs/005-fix-state-wire-center/quickstart.md에 최종 결과 기록


## 8단계: 사용자 이야기 5 - 전역 Snap

- [x] T014 [US5] src/test/kotlin/com/look4counter/ModelSnapSettingsTest.kt에 상태 직렬화·복원·알림·편집기 공유·재열기 검증 추가 (FR-009~011)
- [x] T015 [US5] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelSnapSettings.kt에 애플리케이션 영속 설정과 변경 알림 구현 (FR-009·FR-011)
- [x] T016 [US5] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에 전역 상태 초기화·변경·구독을 연결하고 src/test/kotlin/com/look4counter/ModelEditorFeaturesTest.kt에서 전역 설정 테스트 격리 (FR-010~011)
- [x] T017 전역 Snap 및 기존 Snap 검증을 실행하고 specs/005-fix-state-wire-center/quickstart.md에 결과 기록 및 수렴 점검

후속 전역 설정은 T014 → T015 → T016 → T017 순서로 진행한다.

