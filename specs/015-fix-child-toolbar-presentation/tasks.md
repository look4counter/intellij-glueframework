# 작업 목록: Child 생성 툴바 초기화 오류 수정

## 1단계: 준비

- [x] T001 specs/015-fix-child-toolbar-presentation/research.md에 SDK 갱신 경로와 커스텀 UI 충돌 원인을 기록한다.

## 2단계: 기반

- [x] T002 specs/015-fix-child-toolbar-presentation/contracts/toolbar.md에 표시 갱신과 UI 보존 계약을 정의한다.

## 3단계: 사용자 이야기 1

- [x] T003 [US1] src/test/kotlin/com/look4counter/ChildCreationToolbarTest.kt에 등록된 컴포넌트·복수 인스턴스·상태 위임 검증을 추가하고 수정 전 오류를 재현한다. FR-001~004, SC-001~002.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt의 SplitButtonAction 상속을 커스텀 액션으로 교체한다. FR-001~003.
- [x] T005 [US1] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt의 컨트롤 타입 기대를 새 계약으로 갱신하고 메뉴·텍스트·생성 동작 회귀를 유지한다. FR-002~003.

## 4단계: 검증 및 마무리

- [x] T006 specs/015-fix-child-toolbar-presentation/quickstart.md에 관련 회귀·신규 IDE 로그 구간·설치용 ZIP·수렴 점검 결과를 기록한다. FR-004, SC-001~003.

## 의존성과 구현 전략

T001 → T002 → T003 → T004 → T005 → T006 순서로 수행한다. 같은 생성 컨트롤을 변경하므로 구현은 순차 진행한다. 사용자 이야기 1 전체가 최소 제공 범위다. 서로 다른 기존 회귀 클래스는 일괄 실행할 수 있다.

## 5단계: 표시 이름과 안내 정리

- [x] T007 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt의 Empty Activity·Activity 추가·Activity 선택 및 네 버튼 마우스 안내를 FR-005의 문자열로 갱신한다.
- [x] T008 [US1] src/test/kotlin/com/look4counter/ChildCreationToolbarTest.kt 및 ModelDiagramPropertiesTest.kt의 기존 이름 기대와 액션 안내 검증을 갱신하고 툴바·속성 회귀 및 패키지 검증 후 quickstart.md에 기록하고 수렴을 확인한다. FR-005, SC-002.

T007 → T008 순서로 수행한다. 문구 변경에 새 테스트 클래스를 추가하지 않는다.

