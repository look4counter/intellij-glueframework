# 구현 계획: Resize 좌표 보정과 Magnetic 원복

## 기술 맥락과 요약

기존 Kotlin/JVM, IntelliJ Platform, Gradle 및 JUnit을 유지한다.
DiagramGeometry의 상태 외곽 연결을 보존하고 WireMagnet의 직전 후보 우선순위 변경을 원복한다.
DiagramEdits.relocateNodes에서 Resize된 끝 노드의 중심 차이만큼 각 꺾임점의 해당 상대 벡터를 뺀다.
ModelDiagramCanvas는 이미 relocateNodes를 미리보기에 사용한다.
ModelDesignEditorProvider.editNodes는 보정된 firstDimension과 secondDimension도 기존 XML 태그에 기록한다.

## 헌장 확인

헌장은 미작성 템플릿이다. 한글 문서와 전체 speckit 절차를 준수한다. 설계 전후 위반은 없다.

## 단계와 대상

1. ModelEditorFeaturesTest.kt의 직전 Magnetic 우선순위 검사를 기존 최근접 후보 검사로 바꾸고 WireSnap.kt를 원복한다.
2. ModelDiagramTest.kt에 좌우·시작/끝·다중 끝점 Resize의 절대 꺾임점 보존 검사를 추가한다.
3. DiagramEdits.kt에서 크기가 바뀐 노드만 중심 변화량으로 상대 좌표를 보정한다. 일반 이동은 기존 보간 동작을 유지한다.
4. ModelDesignEditorProvider.kt에서 보정된 꺾임점 상대 좌표를 기존 태그에 기록한다. weight와 사용자 속성은 수정하지 않는다.
5. XML 변경 기대값을 새 저장 동작에 맞추고 미리보기·저장·재열기·실행 취소를 검증한다.

research.md, data-model.md, contracts/geometry.md와 quickstart.md를 따른다.
추가 의존성이나 데이터 마이그레이션은 없다.

## Snap 토글 통합 설계

WireSnap.kt를 WireSnap.kt로 바꾸고 속성·액션을 snapEnabled·snapAction으로 통일한다.
ChildCreationToolbar의 표시명은 Snap이며 설명에 Wire와 Child 정렬을 함께 명시한다.
ModelDiagramCanvas.previewChildDrag의 Alt 우회 조건에 Snap 꺼짐을 추가하고 최소 너비 제한은 유지한다.
토글을 끄면 보조선을 즉시 지우고 진행 중 Child 미리보기를 자유 좌표로 다시 계산한다.
ModelDiagramAlignmentTest의 기존 정렬 검증은 Snap을 명시적으로 켜고, 새 검증은 양 상태와 이동·Resize·Alt·진행 중 끄기를 비교한다.
ModelEditorFeaturesTest에서 표시명과 토글 연결을 확인한다. Wire 최근접 후보 규칙은 유지한다.


## 전역 Snap 구현 계획

애플리케이션 범위 PersistentStateComponent인 ModelSnapSettings.kt를 추가하여 enabled를 별도 설정 파일에 저장한다.
애플리케이션 메시지 버스의 Snap 변경 Topic으로 모든 편집기를 동기화한다.
ModelDesignEditorProvider는 저장된 설정으로 Canvas를 초기화하고 툴바 변경은 전역 설정에 쓴다.
각 편집기는 자신의 Disposable로 구독하고 UI 갱신은 EDT에서 수행한다. Canvas의 기존 Snap setter로 드래그 중 끄기의 보조선 정리도 유지한다.
프로젝트별 ModelEditorSettings와 모델 XML은 그대로 둔다. 편집기 테스트는 전역 상태를 저장·복원하여 서로 영향을 주지 않게 한다.
ModelSnapSettingsTest.kt에서 영속 상태 직렬화·복원, 구독 해제, 두 파일의 툴바와 Canvas 동기화 및 재열기를 검증한다.
