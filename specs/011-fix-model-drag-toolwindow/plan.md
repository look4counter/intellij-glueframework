# 구현 계획: 모델 드래그와 Tool Window 충돌 수정

**날짜**: 2026-10-26 | **명세**: [spec.md](spec.md)

## 기술 배경과 개요

Kotlin 2.3.20, IntelliJ 2025.3.6.1, XML PSI와 기존 Canvas 콜백을 유지한다. editNodes/editWire 완료 처리를 기존 deferModelEdit로 예약하고 드래그 시작 문서 stamp를 명시적으로 캡처한다. Tool Window의 공통 ID를 com.look4counter.intellij-glueframework.tools로 변경하고 stripe 리소스를 맞춘다.

## 규약 확인

헌법은 미작성 템플릿이므로 구체 규칙 검사를 생략한다. 한글 문서와 speckit 순서를 따른다. 설계 전후 위반 없음.

## 조사와 설계

1. 연구 에이전트가 Canvas.mouseReleased → editNodes/editWire → commitDocument의 잠금 없는 경로를 확인했다.
2. 두 함수에 expectedStamp 기본 인자를 추가한다. 첫 진입에서 deferModelEdit로 처리하며 재호출에 캡처한 stamp를 전달한다.
3. 기존 문서 비교를 expectedStamp로 바꿔 다음 제스처가 moveStamp를 바꿔도 이전 요청을 보존한다. 나머지 편집 제한과 쓰기 명령은 유지한다.
   편집 제한으로 취소할 때 원문을 다시 표시해 읽기 전용 전환 후 미리보기가 남지 않게 한다.
4. plugin.xml의 toolWindow ID와 표시 이름 키를 플러그인 고유 이름으로 변경한다. 기존 Factory와 화면은 유지한다.
5. 실제 잠금 없는 Child·Wire 마우스 드래그, 크기 조절, 원문 변경 후 취소를 검증하고 전체 모델 드래그·속성 테스트를 실행한다.
6. 실제 ToolWindowManager에 공통 이름과 고유 ID를 함께 등록해 충돌 없이 기존 팩토리가 콘텐츠를 만드는지 검증한다.
7. ZIP의 descriptor·class·bundle을 검사한다.

## 구조

- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt: 드래그 완료 가드와 stamp 보존.
- src/main/resources/META-INF/plugin.xml: 고유 Tool Window ID.
- src/main/resources/messages/MyMessageBundle.properties: 표시 이름.
- src/test/kotlin/com/look4counter/ModelDiagramTest.kt: 실제 드래그 회귀.
- src/test/kotlin/com/look4counter/ToolWindowRegistrationTest.kt: 등록 충돌·콘텐츠·표시 이름 검증.

## 복잡성 점검

새 외부 의존성이나 저장 형식 변경 없이 기존 예약 도우미와 모델 편집 명령을 재사용한다.
