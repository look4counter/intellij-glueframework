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

## 패닝 표시 경계 보완 계획

미니맵의 모델 좌표와 고정 투영은 유지한다. ModelDiagramCanvas.drawMiniMap에서 표시 사각형만 콘텐츠 영역의 안쪽 1픽셀 여백으로 제한하고 콘텐츠 클립을 유지한다. 큰 이동에서는 표시가 가장자리에 머문다. ModelDiagramMiniMapTest에서 네 방향의 파란 픽셀 위치와 표시 유지를 검사한다.

## 종료 상태 중심 보정 계획

ModelDiagramCanvas.drawNode의 FinalState 안쪽 원 지름 15를 유지하면서 오프셋을 (20 - 15) / 2인 2.5로 보정한다. 메인 화면과 미니맵, 패닝 이미지가 같은 함수를 공유하므로 모두 반영된다. 작은 좌표 보정은 중심 산술 검토와 기존 캔버스 회귀 테스트로 확인한다.

## 종료 상태 픽셀 정렬 계획

ModelDiagramCanvas.drawNode의 FinalState 렌더링에 한해 KEY_STROKE_CONTROL의 VALUE_STROKE_PURE를 적용하여 윤곽과 채움이 동일한 기하 좌표를 사용하게 한다. 기존 그래픽 상태는 복원한다. ModelDiagramPerformanceTest에 실제 픽셀의 내부 원·외부 원 중심을 비교하는 회귀 검증을 추가해 수정 전 재현을 확인한다. 외부 라이브러리나 API 변경은 없다.
