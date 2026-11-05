# 구현 계획: 화면 이동 이미지 재사용

작성일: 2026-11-06. 명세: [spec.md](spec.md)

## 요약

ModelDiagramCanvas의 도형 렌더링을 별도 함수로 분리한다. 첫 패닝 이벤트 직전에 주변 영역을 포함해 한 번 렌더링하고 이후에는 이동한 이미지를 표시한다. 영역 밖은 벡터 렌더링으로 보충한다. 좌표와 미니맵은 별도로 그린다.

## 기술 맥락

Kotlin, Swing Graphics2D, BufferedImage, 기존 IntelliJ 플랫폼 테스트를 사용한다. 새 의존성과 영구 저장은 없다. 화면 주변에 반 화면 여백을 두고 최대 1600만 픽셀로 해상도를 제한한다. 패닝 이미지 생성은 제스처당 최대 1회이다.

## 헌장 점검

헌장은 미작성 템플릿으로 규범 검사를 생략한다. 한글 문서와 speckit 순서를 준수한다. 설계 후 재검사도 통과했다.

## 구조와 단계

- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt: 이미지 수명과 화면 렌더링 분리.
- src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt: 이미지 재사용, 종료 및 바깥 영역 검증.
- 명세 디렉터리의 research.md, data-model.md, contracts/navigation.md, quickstart.md 참조.

순서: 동작 회귀 검증 작성 → 이미지 수명과 렌더링 통합 → 관련 테스트 → 수렴 점검.
패닝 이미지에 시작 시 미니맵 투영도 보관한다. 이동 중 배경 투영은 재사용하고 현재 화면 영역만 갱신하여 미니맵 전체 렌더링을 방지한다.
