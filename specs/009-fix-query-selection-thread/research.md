# 조사 결과

## 결정과 근거

목록 선택 이벤트가 실행하는 flushPending → refresh 전체를 쓰기 의도 잠금과 쓰기 안전 문맥에서 처리한다. 잠금 또는 문맥이 없으면 Application.invokeLater로 재예약한다.

연구 에이전트가 읽기 전용으로 확인한 경로는 QueryDesignEditor의 목록 선택 리스너 → flushPending → QueryMapDocumentModel.apply의 commitDocument 또는 refresh의 commitDocument다. 변경이 없어도 커밋 호출 자체가 잠금을 검사하므로 오류가 발생한다.

선택 이벤트 시점의 SmartPsiElementPointer를 보존하고 요청 순번으로 이미 대체된 선택을 무시한다. 기존 자동 갱신이 먼저 실행돼도 마지막 선택 대상을 복구한다. 폐기된 편집기는 실행하지 않는다.

## 대안

- refresh만 보호: 앞선 flushPending의 커밋 오류가 남으므로 제외.
- 기존 scheduleRefresh만 호출: 기본 선택 포인터가 이전 Query라 선택이 돌아갈 수 있으므로 제외.
- 전체 UI를 쓰기 액션으로 감싸기: 불필요하게 강한 잠금을 사용하므로 제외.
