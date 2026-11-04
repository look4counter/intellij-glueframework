# 구현 계획: Activity 미리보기 드래그

**작성일**: 2026-11-04 | **명세**: [spec.md](spec.md)

## 요약

미리보기 위치 갱신을 공통 함수로 묶어 이전·현재 노드의 화면 범위를 합친 부분만 다시 그린다. 기존 클립 기반 노드·연결선 생략과 미니맵 캐시를 그대로 사용한다. 추가 모드 드래그는 일반 도형 이동보다 먼저 처리하고 유효한 놓기에서 한 번 제출한다. 추가 모드 진입 시 기존 도형 드래그의 클릭 억제 상태를 초기화한다. 레이아웃 전 합성 입력도 기존 동작을 유지한다.

## 기술 환경

- 언어: Kotlin, JDK 21, Swing, IntelliJ Platform 2025.3.6.1.
- 저장: 기존 XML 및 실행 취소 경로 유지. 미리보기 이동에는 저장 없음.
- 검증: BasePlatformTestCase와 BufferedImage 비교, RepaintManager 갱신 영역 관찰.
- 대상: Windows IntelliJ 플러그인, 2,000개 노드 및 확대·축소 화면.
- 제약: 화면 밖·미니맵 놓기 차단, 취소 및 중복 이벤트 보호.

## 헌장 확인

헌장은 미작성 템플릿으로 실질 조항이 없다. AGENTS.md의 한글 문서 및 전체 speckit 절차를 준수한다. 설계 전후 위반 없음.

## 구조 및 단계

- 문서: specs/018-fix-activity-ghost-drag/ 아래 명세, 조사, 데이터 모델, 계약, 검증 안내, 작업 목록.
- 구현: modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt.
- 검증: src/test/kotlin/com/look4counter/ModelEditorCreationTraceTest.kt 및 ModelDiagramPerformanceTest.kt.
- 순서: 부분 갱신과 드래그 처리 → 회귀 검증 → 플러그인 패키징 → 수렴 확인.

## 복잡성

추가 캐시와 스레드를 도입하지 않는다. 기존 클립 최적화를 활용하므로 화면 변환·스타일 변경에 대한 별도 캐시 무효화가 필요 없다.
