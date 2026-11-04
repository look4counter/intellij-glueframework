# 데이터 모델

- ModelSnapState: 기존 enabled와 독립 orthogonalEnabled를 유지하며 신규 상태 기본값은 false다.
- OrthogonalWireSnap: 현재 유지하는 x·y 기준 좌표. 원래 포인터가 화면상 진입 범위 안이면 붙고 유지 범위를 벗어나면 해제한다.
- Wire 드래그 포인터: 마지막 화면 좌표와 Alt 상태. 설정 변경·키 입력 시 같은 위치를 재평가한다.

드래그 시작·종료·취소 시 기준 좌표를 비운다. 영속 Wire 좌표·가중치·연결 참조는 기존 형식을 따른다.
