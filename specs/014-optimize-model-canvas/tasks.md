# 작업 목록: 대규모 모델 화면 최적화

## 1단계: 준비

- [x] T001 specs/014-optimize-model-canvas/research.md에 반복 계산 경로와 호환성 제약을 조사한다.
- [x] T002 src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt에 합성 모델 측정과 회귀를 추가하고 수정 전 기준을 기록한다. FR-006, SC-004.

## 2단계: 기반

- [x] T003 specs/014-optimize-model-canvas/contracts/canvas.md에 캐시 무효화·화면 경계·부분 갱신 계약을 정의한다.

## 3단계: 사용자 이야기 1

- [x] T004 [US1] src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt에 미니맵 재사용·탐색·스타일 무효화 검증을 추가한다. FR-001~002, SC-001.
- [x] T005 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramMiniMap.kt와 ModelDiagramCanvas.kt에 범위·투영·이미지 캐시를 구현한다. FR-001~002.
- [x] T006 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt의 설정 변경에 스타일 무효화를 연결한다. FR-002.

## 4단계: 사용자 이야기 2

- [x] T007 [US2] src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt에 그리기 대상 수·화면을 통과하는 Wire·라벨·부분 클립 회귀를 추가한다. FR-003, SC-002.
- [x] T008 [US2] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 경계에 따른 표시 대상 필터링을 구현한다. FR-003, FR-005.

## 5단계: 사용자 이야기 3

- [x] T009 [US3] src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt에 무관한 Wire·경로 재사용과 연속 드래그·취소 회귀를 추가한다. FR-004~005, SC-003.
- [x] T010 [US3] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramBoundsIndex.kt에 부분 범위 갱신을 구현한다. FR-004.
- [x] T011 [US3] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 연결 인덱스·경로 재사용·변경 영역 미니맵 갱신을 연결한다. FR-001, FR-004~005.

## 6단계: 검증과 마무리

- [x] T012 specs/014-optimize-model-canvas/quickstart.md에 전후 측정·관련 회귀·설치용 ZIP·수렴 점검 결과를 기록한다. FR-006, SC-001~004.

## 의존성과 구현 전략

T001 → T002 → T003 이후 사용자 이야기 1 → 2 → 3 순서로 구현한다. 각 이야기의 회귀를 먼저 작성하고 검증한다. 마지막에 전체 관련 회귀와 측정을 수행한다. 같은 Canvas를 수정하므로 구현은 순차 수행한다. 기존 테스트 클래스 검토는 독립적으로 가능하다. 최소 제공 단위는 사용자 이야기 1이며 승인된 세 단계를 모두 완료한다.

## 7단계: 수렴 보완

- [x] T013 src/test/kotlin/com/look4counter/ModelServiceDirectEditorTest.kt의 직접 편집·복사·삭제 검증에 기존 Commit 동작을 반영하고 미적용 상태와 Commit 후 저장·실행 취소를 확인한다. FR-005, SC-004. 현재 저장 계약과 기존 검사 기대값의 불일치에 따른 검증 부분 미충족이다.

## 8단계: 사용자 이야기 3의 저성능 그래픽 환경 보완

- [x] T014 [US3] src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt에 이동·크기 조절·Wire 편집의 부분 갱신 면적과 여러 배율의 픽셀 동일성, 안내선 제거 회귀를 추가한다. FR-005, FR-007, SC-005.
- [x] T015 [US3] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 부분 변경의 이전·이후 경계와 미니맵을 갱신하고 중복 전체 갱신을 제거한다. 안내선은 안전한 전체 갱신을 유지한다. FR-007.
- [x] T016 specs/014-optimize-model-canvas/quickstart.md에 관련 회귀·합성 모델 측정·설치 ZIP 및 수렴 결과를 기록한다. FR-006, SC-004~005.

T014 → T015 → T016 순서로 기존 세 이야기의 구현을 보완한다. 최소 제공 범위는 안내선 없는 이동이며 크기 조절·Wire 편집까지 완료한다. 동일 파일 변경이므로 순차 구현한다. 테스트 결과 확인과 문서 검토는 독립적으로 가능하다.

## 9단계: 수렴 보완

- [x] T017 src/test/kotlin/com/look4counter/ModelEditorRefreshTest.kt의 비동기 생성 회귀에서 큐 처리 후 첫 요청이 실행되고 취소한 두 번째 요청은 실행되지 않는 현재 계약을 검증한다. 수정 전 코드에서도 재현된 잘못된 개수 기대값을 수정한다. FR-005, SC-004의 검증 부분 미충족.
