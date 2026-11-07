# 검증 안내

모델을 열어 좌측 상단에는 기존 속성만 남고 미니맵 아래에는 x·y 좌표가 표시되는지 확인한다. 우클릭 화면 이동·미니맵 클릭/드래그·줌·크기 변경 후 위치와 값 갱신을 확인한다. 빈 모델·작은 화면에서 미니맵이 없으면 좌표도 표시하지 않는다.

기존 로컬 JBR·Gradle 캐시로 미니맵·탐색·속성·성능 관련 검증과 ZIP·구조 검사를 실행한다. IDE 소켓 제약으로 검색 옵션 생성은 제외하며 실제 사용자 IDE 수동 확인과 자동 렌더링을 구분한다.

## 자동 검증 결과: 2026-11-08

- 미니맵·탐색·속성·성능 관련 75개 테스트가 통과했다. 실패·오류·건너뜀은 각각 0개다. 새 렌더링 검증에서 좌측 상단 좌표 제거, 미니맵 아래 표시, 실제 우클릭 팬 중 값 갱신과 모델 비변경을 확인했다.
- 자동 렌더링 이미지 `.gradle-validation/model-coordinates-minimap.png`를 확인해 미니맵 아래 `x: 60, y: 40`이 표시되는 것을 확인했다. 실제 사용자 IDE 수동 조작은 수행하지 않았다.
- `test --tests 'com.look4counter.ModelDiagramMiniMapTest' --tests 'com.look4counter.ModelEditorFeaturesTest' --tests 'com.look4counter.ModelDiagramPropertiesTest' --tests 'com.look4counter.ModelDiagramPerformanceTest' buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. 기존 IDE 소켓 제약으로 검색 옵션 생성만 제외했고 ZIP·SHA-256을 갱신했다.
- 근거: `.gradle-validation/model-coordinates-final.log`, `build/reports/model-coordinates-test-summary.json`.
- 수렴 검토에서 요구사항 4개·성공 기준 2개와 화면 좌표계·캐시 밖 갱신·기존 탐색 보존을 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 0개이며 새 수렴 작업은 없다. 헌장은 미작성 템플릿이며 확장 훅은 없다.

## 후속 전체 회귀

설정 옵션 제거 작업과 함께 최종 전체 테스트 512개·설치 ZIP·구조 검사가 통과했다. 기존 ModelDiagramTest의 좌측 상단 좌표 기대값은 폐기하고 독립 연결선 라벨 검증은 유지했다. 좌표 위치와 이동 중 갱신은 ModelDiagramMiniMapTest에서 배경 도형의 영향 없이 검증한다. 최종 근거는 `.gradle-validation/model-option-removal-final.log`와 `build/reports/model-option-removal-test-summary.json`이다.
