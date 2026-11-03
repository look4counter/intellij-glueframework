# 조사 결과

## 드래그 실행 경로

Canvas.mouseReleased는 미리보기 상태를 정리한 후 onWireEdited 또는 onNodesMoved/onNodesResized를 호출한다. 기존 editWire와 editNodes는 commitDocument 전에 쓰기 의도 잠금을 확보하지 않는다. 따라서 이전 속성 대화상자와 같은 오류가 발생한다. 기존 deferModelEdit가 안전 문맥과 폐기 검사를 제공하므로 재사용한다.

## 문서 기준 보존

전역 moveStamp는 다음 제스처가 갱신할 수 있으므로 요청 인자로 고정한다. 예약 실행 시 현재 문서 stamp와 고정한 값을 비교한다. 전체 UI를 쓰기 액션으로 감싸거나 XML 변경을 무조건 허용하는 방식은 제외한다.

전체 회귀 검증에서 canEditStructure가 false이면 문서는 보호되지만 미리보기가 남는 기존 취소 경로를 확인했다. 제한으로 취소할 때 refresh로 원문을 다시 표시하여 문서와 화면을 일치시킨다.

## Tool Window

사용자가 직접 원인으로 MyToolWindow ID 중복을 제공했다. 저장소의 등록은 한 곳뿐이고 이름은 기본 템플릿의 공통 이름이다. 창을 제거하거나 예외를 삼키는 대신 플러그인에 고유한 ID로 변경해 기능을 보존한다. 표시 이름 키도 같이 바꾼다.
