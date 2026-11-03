# 구현 계획: Query 목록 아이콘 편집

**날짜**: 2026-10-26 | **명세**: [spec.md](spec.md)

## 개요와 기술 배경

Kotlin 2.3.20, IntelliJ 2025.3.6.1, Swing, XML PSI와 기존 Query 문서 모델을 사용한다. 목록 위에 기존 플랫폼 아이콘을 사용하는 작은 버튼 줄을 배치한다. 추가 팝업은 DialogWrapper 및 테스트 대체 가능한 프로젝트 서비스로 제공한다.

## 규약 확인

헌법은 미작성 템플릿이다. 한글 문서와 speckit 순서를 따른다. 설계 전후 위반 없음.

## 조사 및 설계

1. 추가 입력은 ID·Description·ResultType 문자열과 IsNamed 체크박스로 구성한다. 빈 ID·중복 ID를 거부한다.
2. QueryMapDocumentModel에서 단일 쓰기 명령으로 추가·삭제·복제·이동을 수행하고 결과 선택 포인터를 반환한다.
3. 복제·이동은 Query 태그 전체 복사로 CDATA·내부 주석·확장 속성을 보존한다. 루트와 다른 형제 요소는 다시 직렬화하지 않는다.
4. QueryDesignEditor는 대기 편집 반영 후 구조 명령과 화면 갱신을 수행한다. 쓰기 잠금·안전 문맥이 없으면 전체 요청을 예약한다.
5. 팝업 중 문서 변경 시 저장을 중단한다. 버튼 상태는 문서·선택·목록 경계에 맞춘다.
6. 실제 버튼 테스트, 팝업 검증, Undo/Redo, XML 보존 및 기존 Design·SQL 테스트를 실행하고 설치용 ZIP을 빌드한다.

## 구조

- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt: 아이콘 버튼과 실행 경로.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryMapDocumentModel.kt: 구조 편집 명령.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryCreationDialog.kt: 추가 팝업과 입력 모델·서비스.
- queryeditor/src/main/resources/META-INF/queryeditor.xml: 프로젝트 서비스 등록.
- src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt: 버튼·팝업·문서 통합 검증.

## 복잡성 점검

새 외부 의존성과 저장 형식 변경이 없다. 기존 PSI·쓰기 명령·문서 갱신 구조를 재사용한다. 연구 에이전트가 로컬 SDK 아이콘과 PSI 복사·이동 방식을 확인했다.

## 버튼 크기 보완 계획

QueryDesignEditor의 다섯 JButton에 JBUI.size(32, 32)를 선호·최소 크기로 지정하고 margin은 JBUI.insets(4)로 설정한다. 아이콘 크기와 실행·활성화 처리는 유지한다. 새 기술 조사가 필요하지 않다. 기존 렌더링 높이를 36으로 맞춰 전체 버튼을 확인하고 아이콘·버튼 동작 테스트와 ZIP 빌드를 수행한다.
