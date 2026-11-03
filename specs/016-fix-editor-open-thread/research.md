# 조사 결과

쿼리의 navigateTo → selectQueryAtOffset은 commitDocument를 즉시 호출한다. selectQuery도 flushPending과 refresh를 즉시 실행한다. 기존 목록 선택에는 쓰기 의도 잠금·트랜잭션 가드가 있지만 외부 탐색에는 없어 찾기 결과 진입 시 누락된다. 초기 scheduleRefresh는 비모달 애플리케이션 큐에서 실행된다.

모델의 생성자 refresh와 selectNotify는 이미 잠금·쓰기 안전성을 검사하고 requestRefresh로 예약한다. 서비스 어댑터도 이 갱신 안에서 초기화된다. 모델 회귀 검증으로 기존 보호를 확인한다.

Java 참조 문서 생성과 소스 편집기 위임, 열기 리스너 정규화도 조사 대상이다. 실제 잠금 없는 생성 테스트로 추가 결함을 확인한다. 쓰기 액션으로 편집기 전체를 감싸는 방식은 과도한 잠금과 읽기 잠금 승격 위험이 있어 채택하지 않는다.

## 최종 재현 근거

잠금 해제 테스트에서 QueryDesignEditor.selectQuery → refresh → commitDocument가 Access is allowed from write thread only 오류를 발생시켰다. QueryMapDocumentModel과 ModelDesignEditor의 생성 중 getDocument는 읽기 접근 오류를 발생시켰다. 생성 제공자의 WriteIntentReadAction 적용과 탐색의 안전한 예약으로 모두 해소했다. Source 제공자, QueryNavigation.open과 ServiceFiles.open도 동일 진입 경계에서 보호했다.

## 메서드 누락 오류 조사

결정: 네 생성 제공자에서 Application.runWriteIntentReadAction(ThrowableComputable)을 직접 호출한다.
근거: 캐시 SDK 2025.3.6.1의 javap 결과는 WriteIntentReadAction.compute(ThrowableComputable)을 포함한다. 현재 공식 소스는 해당 오버로드를 computeThrowable로 변경했다. 기존 두 타입 인자의 Kotlin 호출은 변경된 오버로드를 참조하므로 최신 IDE에서 바이너리 연결 오류가 날 수 있다. Application의 위임 대상 서명은 양쪽에 존재한다. 실제 사용자 IDE 버전은 미확인이므로 원인 판단은 서명 차이에 근거한 추정이다.
대안: 단일 타입 인자의 compute(Computable)도 가능하지만 중간 정적 래퍼 대신 동일한 잠금 구현에 직접 위임하는 쪽을 선택했다. 잠금 제거는 기존 스레드 오류를 되살릴 수 있어 제외했다.
공식 근거: [쓰기 의도 읽기 액션 소스](https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/core-api/src/com/intellij/openapi/application/WriteIntentReadAction.java), [애플리케이션 계약](https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/openapi/application/Application.java).
