# 작업 목록: 현재 직선 위치에서 bendpoint 병합

## 1단계: 준비

- [x] T001 specs/039-fix-bendpoint-straight-merge/spec.md의 사용자 확인 및 명세 품질 검토를 완료한다.

## 2단계: 기반

- [x] T002 specs/039-fix-bendpoint-straight-merge/plan.md와 설계 문서를 작성하고 요구사항·작업 일관성을 분석한다.

## 3단계: 사용자 이야기 1

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelDiagramTest.kt 및 ModelEditorFeaturesTest.kt에 가로·세로의 다른 유효 위치 병합과 XML·실행 취소 회귀를 추가하고 이전 접점만 기준으로 삼던 기대를 현재 위치 계약으로 갱신해 수정 전 실패를 확인한다. FR-001~003, SC-001~002.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt의 마지막 Activity bendpoint 제거 후보에 현재 위치 기준을 적용한다. FR-001~002.

## 4단계: 검증

- [x] T005 specs/039-fix-bendpoint-straight-merge/quickstart.md에 실제 기하·편집·저장 회귀 결과를 기록하고 수렴 점검한다. FR-003, SC-001~002.

## 의존성 및 구현 전략

T001 → T002 → T003 → T004 → T005 순서다. 사용자 이야기 1이 최소 완성 범위이며 동일 소스와 수정 전후 검증에 의존하므로 병렬 작업은 없다.



