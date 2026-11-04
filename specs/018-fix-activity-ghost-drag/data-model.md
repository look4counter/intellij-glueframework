# 데이터 모델

영속 데이터 형식은 바꾸지 않는다.

- creationPoint: 현재 미리보기의 모델 좌표. 영역 이탈 시 비운다.
- creationPressPoint 및 creationPressRevision: 누름으로 시작한 제스처와 모드 세대.
- creationReleaseHandledAt 및 creationReleaseHandledPoint: 놓기 후 클릭 중복 방지.
- 갱신 범위: 모델 좌표를 origin·pan·zoom으로 화면 좌표로 바꾸고 외곽선 여백을 더한다.

상태 전이: 추가 모드 → 누름 → 이동 및 부분 갱신 → 유효한 놓기에서 한 번 제출. 취소·모드 변경·영역 이탈은 제출하지 않는다.
