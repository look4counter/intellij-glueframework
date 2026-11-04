# 구현 계획: Orthogonal Wire 자석

**작성일**: 2026-11-04 | **명세**: [spec.md](spec.md)

## 요약 및 기술 환경

Kotlin, JDK 21, IntelliJ Platform 2025.3.6.1, Swing을 사용한다. 기존 Wire 경로 및 저장 형식을 유지한다. ModelSnapSettings에 독립 Orthogonal 상태를 추가하고 도구 모음과 각 캔버스에 전파한다.

## 헌장 확인

헌장은 미작성 템플릿이므로 실질 조항 검사를 생략한다. AGENTS.md의 한글 문서 및 speckit 전체 절차를 준수한다. 설계 전후 위반 없음.

## 구조와 설계

- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelSnapSettings.kt: 기본 꺼짐 상태 및 변경 알림과 복원.
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt: Snap 직후 Orthogonal 토글.
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt: 상태 초기화 및 편집기 간 전파.
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/OrthogonalWireSnap.kt: 가로·세로별 기준 유지와 6px 진입·10px 이탈. 모델 오차에 확대율을 곱해 판정한다.
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt: 꺾임점·중앙 핸들 미리보기와 놓기에 보정 적용. Alt·토글 변경 때 즉시 재계산하고 제스처 경계에서 자석 상태를 초기화한다.
- src/test/kotlin/com/look4counter/OrthogonalWireSnapTest.kt: 기하·실제 드래그·설정·도구 모음·원문 및 실행 취소 검증.

## 기하 규칙

기존 꺾임점은 앞뒤 두 점, 중앙 핸들은 양 끝 점을 기준으로 한다. 끝점에 인접한 구간은 노드 중심 축을 기준으로 하여 접점 재계산 후에도 수평·수직을 유지한다. 명시적 Activity 접점은 실제 경로 좌표를 사용한다. 기존 Snap 결과는 유지하되 자석 임계 판정은 원래 마우스 좌표로 하여 감도를 일정하게 한다.

## 검증 전략

축별 진입·유지·해제·줌·음수 좌표를 검증한다. 실제 중앙·꺾임점 드래그, Alt·토글 해제, 취소, 저장·실행 취소를 확인한다. ModelSnapSettingsTest 및 기존 모델 편집 회귀 테스트와 buildPlugin을 실행한다.

## 복잡성

별도 라우팅 엔진이나 저장 형식 변경 없이 한 드래그의 두 축 좌표만 유지한다. 기존 설정 서비스와 알림 채널을 재사용한다.
