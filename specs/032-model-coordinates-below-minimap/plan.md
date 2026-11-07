# 구현 계획: 미니맵 아래 좌표 표시

## 기술 맥락과 헌장

기존 Kotlin·Swing Graphics2D·ModelDiagramCanvas·DiagramMiniMap을 사용한다. 외부 의존성·저장 형식을 변경하지 않는다. 헌장은 미작성 템플릿이므로 규범 검사를 생략하고 한글 문서·speckit 순서를 준수한다.

## 구현 단계

1. `modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt`: paintComponent의 좌측 좌표 그리기를 제거하고 속성의 첫 줄을 기존 좌표 자리로 올린다. drawMiniMap 뒤에서 미니맵 프레임 아래의 화면 좌표로 기존 pan 문구를 그린다. 좌표는 탐색 이미지 캐시 밖에서 매번 그려 현재 값을 유지한다.
2. `src/test/kotlin/com/look4counter/ModelDiagramMiniMapTest.kt`: 실제 렌더링에서 좌측 좌표 제거·미니맵 아래 표시·pan 변경 시 갱신을 확인한다. 기존 미니맵·속성·탐색·성능 회귀를 실행한다.
3. README·quickstart를 기록하고 buildPlugin·verifyPluginStructure를 실행한다. 기존 IDE 소켓 제약으로 buildSearchableOptions를 제외하고 명시한다.

## 위치와 제약

미니맵 왼쪽 여백에 맞추고 아래 8픽셀 간격에 글꼴 ascent를 더해 baseline을 계산한다. 화면/줌 변환을 되돌린 Graphics2D에서 그려 배율에 따라 좌표 글자 크기가 바뀌지 않는다. 미니맵 크기·캐시·클릭 판정은 변경하지 않는다.
