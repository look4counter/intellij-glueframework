# 구현 계획: 이동한 직선 위치에서 bendpoint 병합

작성일: 2026-11-18 | 명세: [spec.md](spec.md)

## 요약 및 기술 문맥

Kotlin·JDK 21·IntelliJ IDEA 2025.3.6.1 데스크톱 플러그인이다. 기존 XML과 UI를 유지한다. DiagramEdits.dragBendpoint에서 마지막 점 제거 후보가 기존 firstRelativePoint를 사용해 새 위치의 직선과 불일치하는 문제를 수정한다. Activity↔Activity의 마지막 점을 제거한 후보에 현재 포인터를 firstRelativePoint 기준으로 전달하고 기존 경로·거리·관통 판정을 수행한다. 여러 점과 State는 기존 동작을 유지한다.

## 헌장 점검

헌장은 미작성 템플릿이므로 규범 검사를 생략한다. 설계 전후 한글 문서 및 speckit 절차를 준수한다.

## 조사와 설계

추가 기술 의문은 없다. [research.md](research.md), [data-model.md](data-model.md), [입력 계약](contracts/merge.md)을 따른다. 현재 포인터 기준 후보를 계산하므로 임계치나 도형 교차 규칙을 완화하지 않는다.

## 파일 및 검증 순서

1. src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 원래 접점에서 떨어진 가로·세로 좌표 및 실제 XML·실행 취소 회귀를 추가하고 수정 전 실패를 확인한다.
2. modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt의 마지막 점 제거 후보만 보정한다. Provider의 기존 접점 쓰기·bendpoint 하나 삭제·Undo 경로를 재사용한다.
3. ModelDiagramTest 및 Wire·스냅·클립보드 회귀를 실행하고 quickstart.md에 기록한다.

ModelEditorFeaturesTest.kt의 기존 스냅 테스트도 현재 위치에서 직선이면 마지막 점이 제거되는 계약으로 갱신한다. 실제 접점 x가 스냅 결과를 유지하는지 검사하고, 그 외 범위 밖 이동 검증은 기존 테스트로 유지한다.
