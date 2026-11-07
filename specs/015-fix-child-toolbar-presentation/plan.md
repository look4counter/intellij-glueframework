# 구현 계획: Child 생성 툴바 초기화 오류 수정

**작성일**: 2026-10-27 | **명세**: [spec.md](spec.md)

## 요약과 기술 맥락

Kotlin·Swing·IntelliJ Platform 2025.3.6.1 기반이다. ChildCreationToolbar의 텍스트 버튼·종류 화살표·동적 프리셋 메뉴를 유지하면서 커스텀 액션의 갱신 계약을 바로잡는다. 새 의존성과 영속 데이터 변경은 없다.

## 헌법 검사

헌법은 미작성 템플릿이므로 추가 원칙은 없다. 한글 문서와 speckit 절차를 준수한다. 기존 작업과 Canvas 최적화를 보존한다. 설계 전후 위반 없음.

## 조사 및 결정

현재 splitAction은 SplitButtonAction을 상속하지만 createCustomComponent에서 별도 ActionToolbar 컴포넌트를 반환한다. 상속받은 update는 기본 SplitButton으로 캐스팅한 결과를 Presentation.copyFrom에 전달한다. 실제 컴포넌트가 기본 SplitButton이 아니어서 null을 전달하며 등록된 COMPONENT_KEY를 초기화하려고 한다. 기존 IDE 로그에서도 같은 호출 경로가 확인됐다.

SplitButtonAction 상속을 제거하고 DumbAwareAction과 CustomComponentAction을 구현한 명명된 생성 컨트롤 액션을 사용한다. 기존 splitAction 속성 이름과 actionGroup 조회를 유지하여 메뉴 호출자를 보존한다. update는 EDT에서 기존 toggleAction.update에 위임하여 필요한 텍스트·아이콘·선택·활성 상태만 같은 Presentation에 갱신한다. actionPerformed도 기존 토글에 위임한다. createCustomComponent의 내부 툴바 및 메뉴 생성 코드는 유지한다. Presentation.copyFrom이나 COMPONENT_KEY 덮어쓰기는 하지 않는다.

## 파일 범위

- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt: 커스텀 생성 컨트롤 액션과 기존 UI 연결.
- src/test/kotlin/com/look4counter/ChildCreationToolbarTest.kt: 등록 컴포넌트 보존·복수 인스턴스·토글·프리셋·읽기 전용 회귀.
- src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt: 기존 SplitButtonAction 타입 기대를 커스텀 액션 계약으로 갱신하고 실제 텍스트 툴바 검증 유지.

## 검증

실제 커스텀 컴포넌트를 CustomComponentAction.COMPONENT_KEY에 등록한 후 실제 update를 반복한다. 수정 전 오류를 재현하고 수정 후 같은 컴포넌트가 유지되는지 검사한다. 두 툴바가 독립적으로 동작하는지, 종류·프리셋·선택·읽기 전용 상태 및 직접 토글 위임을 검사한다. 기존 속성·생성 추적·새로고침·Snap·Service 직접 편집 회귀를 실행한다. 검증 시작 시점 이후 IDE 로그에서 해당 오류가 없는지 확인하고 설치용 ZIP의 변경 클래스를 검사한다.

## 후속 문구 변경 계획

ChildCreationToolbar.kt의 Activity label, toggleAction의 동적 description, 종류 메뉴·선택 액션 및 네 버튼의 description을 요청 문자열로 바꾼다. 컴포넌트·생성·토글 로직은 유지한다. ChildCreationToolbarTest.kt의 기존 기본 이름 기대를 갱신하고 기존 액션 갱신 검증에서 안내 문구를 확인한다. 해당 툴바와 속성 회귀 및 설치 ZIP·구조 검사를 수행한다. 새로운 테스트 클래스나 의존성·저장 상태는 추가하지 않는다. 헌장은 미작성 템플릿이고 확장 훅은 없다.

