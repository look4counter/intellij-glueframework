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

## 주기적 상태 조회 개선 계획

ServiceDiagramAdapter.changeHighlights에서 편집본 객체·수정 시각·기준 모델 객체를 키로 마지막 비교 결과 하나를 재사용한다. 내용이 바뀌면 재계산하고 markPendingChanges·새 투영 생성·롤백·적용 시 참조를 해제한다. hasPendingChanges와 Commit/Rollback 도구 모음 및 isModified가 같은 결과를 공유한다. ModelDesignEditorProvider의 isPastePossible은 드래그 상태를 먼저 검사하여 클립보드 읽기를 생략한다.

검증: src/test/kotlin/com/look4counter/ModelServiceDirectEditorTest.kt에서 반복 조회의 결과 객체 동일성과 수정 후 새 결과, 원복·롤백의 정확성을 확인한다. src/test/kotlin/com/look4counter/ModelDiagramClipboardTest.kt에서 패닝 중 붙여넣기 조회 및 종료 후 복귀를 검증한다. 기존 성능·서비스·클립보드 테스트와 buildPlugin을 실행한다. 의존성과 외부 API 추가는 없으며 헌장 점검 결과는 동일하다.

## 미니맵 위치 동기화 보완 계획

이 절이 기존 미니맵 고정 투영과 경계 고정 정책을 대체한다. 현재 화면과 캐시된 모델 경계로 표시 투영을 갱신한다. 스냅샷 시작 투영은 배경 이미지 생성에만 사용하고 두 투영의 배율 비율과 오프셋으로 이미지를 변환해 재사용한다. 기존 Kotlin/Swing만 사용하며 헌장 재점검 결과 위반은 없다.

검증: ModelDiagramMiniMapTest에서 네 방향 중간 프레임의 파란 픽셀 이동, 전체 범위, 역방향 복귀, 종료를 확인한다. ModelDiagramPerformanceTest로 재사용을 검증하고 buildPlugin을 실행한다.
작은 모델이 화면 안에 전부 들어오면 자동 맞춤만으로 표시가 콘텐츠를 채울 수 있다. 패닝 시작 투영의 전체 범위에 각 축 반 범위의 여백을 추가한 기준 범위와 현재 화면을 합쳐 투영한다. 시작 시 투영 캐시를 무효화한다.

## 배율 유지 보완 계획

이 절이 이전의 두 배 여백과 패닝 중 자동 맞춤 설계를 대체한다. 스냅샷 시작 투영의 배율·오프셋을 유지하고 modelViewport만 현재 위치로 갱신한다. 배경 이미지는 좌표 변환 없이 그린다. 파란 사각형의 각 경계를 콘텐츠 안쪽 1픽셀 영역으로 개별 제한하여 실제 이동에 따른 잘린 범위를 표시한다. 완전히 벗어난 방향은 2픽셀 최소 표시를 유지한다. 종료 시 투영 캐시를 비워 일반 자동 맞춤으로 복귀한다. Kotlin/Swing과 기존 테스트만 사용하며 헌장은 미작성 템플릿이다.

검증: ModelDiagramMiniMapTest에 시작 전후 동일 투영 검증을 추가하고 기존 연속 이동 및 큰 이동 테스트에서 고정 배율·오프셋과 표시 이동·가시성을 확인한다. 기존 ModelDiagramPerformanceTest의 이미지 재사용·종료 픽셀 일치를 포함한 68개 회귀 테스트를 직접 컴파일·JUnitCore로 실행한다. 기존 빌드 의존성 누락은 결과에 명시한다. 설계 후 헌장 재점검에서 추가 제약은 없다.

## 균등 여백과 패닝 요소 이동 계획

이 절이 이전 패닝 중 도형 위치 고정 정책을 대체한다. DiagramMiniMap.layout의 콘텐츠를 상하좌우 8픽셀 여백으로 통일한다. ModelDiagramCanvas의 패닝 투영은 시작 배율을 유지하고 시작·현재 화면 중심 차이의 절반만큼 표시 중심을 이동한다. 요소는 패닝 방향으로, 화면 표시는 반대 방향으로 같은 거리만큼 움직여 두 표시의 상대 이동이 원래 화면 이동과 일치한다. 시작 시 이동량은 0이며 배율이 변하지 않는다.

캐시 배경은 시작 투영으로 유지한다. 현재·시작 투영 오프셋 차이만큼 Graphics2D 복제본을 평행 이동해 이미지를 표시하며 콘텐츠 클립을 유지한다. 종료 시 기존 정상 렌더링으로 돌아온다. 실제 모델 변경이나 외부 의존성 추가는 없다. 기존 Kotlin/Swing을 사용하며 헌장은 미작성 템플릿이다. 설계 후 규범 재점검 결과 추가 제약은 없다.

검증: ModelDiagramMiniMapTest의 여백 테스트 및 네 방향 요소 표시 픽셀 이동·상대 위치·복귀 검증을 추가한다. 기존 시작 배율·경계 표시·이미지 재사용·종료 픽셀 일치를 포함한 회귀 테스트를 직접 컴파일·JUnitCore로 실행한다. 새 작업은 해당 테스트와 DiagramMiniMap.kt, ModelDiagramCanvas.kt에 한정한다.

## 패닝 중 자동 배율 계획

이 절이 이동 중 고정 배율 설계를 대체한다. 시작·현재 화면의 중간을 따라가는 표시 중심은 유지한다. 현재 모델과 화면의 합집합이 그 중심의 좌우·상하에 모두 들어오는 최대 배율을 계산한다. 미니맵 배율 상한 0.5와 균등 여백을 유지하며 시작 순간과 원점 복귀 시 기존 투영과 일치한다. ModelDiagramCanvas에서 현재 투영을 계산하고 캐시 배경 이미지를 현재·시작 투영의 배율 비율과 오프셋으로 변환하여 그린다. 원래 투영을 사용할 때는 변환 없이 그려 종료 픽셀을 보존한다.

검증 범위: ModelDiagramMiniMapTest의 연속 이동을 현재 투영의 상대 좌표 기준으로 갱신한다. 큰 패닝의 전체 범위 포함과 배율 축소·복귀, 실제 요소의 픽셀 크기 변화를 검증한다. 기존 70개를 포함한 직접 컴파일·JUnitCore 회귀 검증을 수행한다. Kotlin/Swing 외 새 의존성은 없다. 헌장은 미작성 템플릿이며 설계 전후 추가 규범 제약은 없다.
