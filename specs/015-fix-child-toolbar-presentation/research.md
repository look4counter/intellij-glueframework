# 조사 결과

## 결정

기본 SplitButtonAction의 생명 주기를 사용하지 않고 커스텀 UI에 맞는 CustomComponentAction을 사용한다.

## 근거

기본 update는 등록된 컴포넌트가 SplitButton일 것으로 기대한다. 현재 반환하는 컴포넌트는 ActionToolbar이므로 캐스팅 결과가 null이며 Presentation.copyFrom이 등록 컴포넌트를 변경하려고 한다. IDE 로그와 로컬 SDK의 해당 구현을 대조했다.

## 대안

기본 SplitButton으로 UI 전체를 교체하면 이미 합의한 텍스트 버튼과 화살표 구성이 달라져 제외한다. 로그만 억제하거나 갱신 전에 컴포넌트 키를 지우면 원인이 남으므로 제외한다. 기존 토글와 메뉴를 재사용하면 생성 동작을 다시 구현할 필요가 없다.

미해결 기술 항목은 없다.
