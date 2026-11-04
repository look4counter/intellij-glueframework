# 구현 계획: Activity 추가 안전성 및 Snap

**작성일**: 2026-11-04 | **명세**: [spec.md](spec.md)

## 요약 및 기술 환경

Kotlin, JDK 21, IntelliJ Platform 2025.3.6.1 및 Swing을 사용한다. 저장 형식과 모듈 의존성은 변경하지 않는다.

## 헌장 확인

헌장은 미작성 템플릿이므로 실질 조항 검사를 생략한다. AGENTS.md의 한글 문서 및 speckit 전체 절차를 준수한다. 설계 전후 위반 없음.

## 설계와 구조

1. modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt의 createChild 진입부에서 기존 deferModelEdit를 사용한다. 모드 세대를 재검증하여 취소·모드 변경 요청은 버린다.
2. modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt의 미리보기 위치 갱신에서 DiagramAlignment.move를 사용한다. 원래 포인터와 Alt 상태를 보관해 설정 변경에도 위치를 다시 계산한다.
3. 클릭·누름·이동·드래그·놓기에 공통 위치 갱신을 적용한다. 안내선 범위를 이전·현재 Ghost 범위와 합쳐 부분 갱신하며 취소·영역 이탈 시 정리한다.
4. 정렬 계산은 입력 및 설정 변경 시에만 수행하고 ghostNode 조회나 paintComponent에서는 반복하지 않는다.
5. src/test/kotlin/com/look4counter/ModelActivityCreationTest.kt에서 스레드·Snap·수명·원문 및 실행 취소를 검증한다. 기존 모델 편집 테스트도 실행한다.

## 검증 전략

실제 UI 이벤트에서 쓰기 의도 잠금을 해제해 추가를 재현한다. Snap은 중심·모서리·동일 간격, 비활성화·Alt, 확대율 및 Ghost와 확정 위치 일치를 검증한다. 기존 부분 갱신 및 모델 편집 회귀 검증과 buildPlugin을 실행한다.

## 복잡성

기존 스레드 위임 함수와 정렬 알고리즘을 재사용한다. 별도 작업 스레드나 영속 상태는 추가하지 않는다.
