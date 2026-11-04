# 구현 계획: 쿼리 편집기 탭 위치 동기화

**작성일**: 2026-11-04 | **명세**: [spec.md](spec.md)

## 요약

공유 원문 Document에 Design 선택 쿼리의 스마트 포인터와 Source 이탈 커서 오프셋을 보관한다. Source 활성화 시 최신 문서를 커밋하고 선택 포인터 또는 첫 query의 시작으로 이동한다. Design 활성화 시 Source 이탈 위치를 한 번 소비하고 해당 XML 요소 또는 첫 query를 선택한다. ID로 대응시키지 않는다.

## 기술 환경

- Kotlin, JDK 21, IntelliJ Platform 2025.3.6.1, Swing.
- 영속 저장 형식 변경 없음. Document 사용자 데이터에 탐색 상태만 보관한다.
- BasePlatformTestCase에서 실제 공급자의 편집기와 탭 알림을 검증한다.
- Source 로딩 완료 후 커서와 스크롤을 갱신하고 폐기된 편집기 작업을 차단한다.
- 커밋과 Design 수정 반영은 기존 쓰기 안전 모달리티 규칙을 따른다.

## 헌장 확인

헌장은 미작성 템플릿이므로 실질 조항 검사를 생략한다. AGENTS.md의 한글 문서와 전체 speckit 절차를 따른다. 설계 전후 위반 없음.

## 조사 및 설계

1. 기존 QueryNavigation.queryAtOffset의 시작 포함·끝 제외 범위를 재사용한다.
2. QueryDesignEditor.refresh에서 선택 포인터를 보관하고 selectNotify에서 Source 위치를 적용한다.
3. QuerySourceEditor의 selectNotify에서 커서를 이동하고 deselectNotify에서 위치를 보관한다.
4. 활성화 세대와 폐기 검사를 적용해 늦게 실행되는 작업의 덮어쓰기를 막는다.
5. 기존 requestedQuery 외부 탐색은 refresh의 우선순위를 유지한다.

## 구조

- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryTabNavigation.kt: 공유 키 및 안전 실행.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt: 선택 게시와 Source 위치 소비.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QuerySourceEditorProvider.kt: 커서 보관과 이동.
- src/test/kotlin/com/look4counter/QueryTabNavigationTest.kt: 양방향·대체·편집·경계·수명 검증.

## 검증 전략

새 회귀 테스트와 기존 QueryDesignEditorTest, EditorMultiViewTest, EditorOpenThreadTest, JavaQueryNavigationTest를 실행하고 buildPlugin으로 설치용 ZIP을 생성한다. 실행 환경의 캐시 문제는 이전 작업의 임시 오프라인 초기화 스크립트를 재사용해 해결한다.

## 복잡성

프로젝트 서비스나 이벤트 버스를 추가하지 않고 문서 수명에 연결된 두 개의 키를 사용한다. 기존 파일 형식과 외부 탐색 계약을 유지한다.
