# 조사 결과

## 결정

편집 종료를 수행하는 result는 Apply·선택 전환에서 유지하고, 별도 snapshot은 셀 편집기의 현재 값을 복사본에 합성한다.

## 근거

isModified → value → saveCurrent → result → stopCellEditing 경로가 주기적인 편집 종료를 만든다. 조회 시 목록 변경 이벤트도 피해야 한다.

## 대안

조회에서 편집 중인 값을 무시하면 Apply 활성화가 늦어지므로 제외한다. 모든 result 호출을 순수 조회로 바꾸면 기존 대화상자 확정 계약까지 바뀌므로 제외한다. 미해결 조사 항목은 없다.
