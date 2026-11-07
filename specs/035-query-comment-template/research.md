# 조사 결과

- 결정: 기존 프로젝트 서비스 패턴 및 설정 수명주기를 재사용한다. 근거: QueryFormattingSettings와 GlueQueryEditorConfigurable에 동등한 기능이 있다. 대안: 전역 설정은 프로젝트별 기존 동작과 달라 제외한다.
- 결정: QueryEdit를 통한 SQL 변경. 근거: CDATA 분할·충돌 검사·Source 동기화·실행 취소를 이미 제공한다. 대안: 편집기 버퍼 직접 변경은 지연 저장 충돌 가능성이 있어 제외한다.
- 결정: 달러와 샤프 표기를 모두 지원하고 날짜 형식 오류는 보존한다. 근거: 사용자 예시와 미지원 변수의 안전한 보존. 대안: 모든 변수를 날짜로 해석하면 일반 변수를 잘못 치환할 수 있다.
- 미해결 기술 질문은 없다.

## 상위 설정 화면 조사

- 결정: IntelliJ groupConfigurable 확장과 groupId를 사용한다. 근거: 로컬 SDK의 PlatformExtensions.xml에서 기본 appearance·editor·tools 메뉴가 동일 확장을 사용한다. ConfigurableEditor.createDefaultContent는 ConfigurableGroup.description과 직계 자식으로 기본 링크 목록을 생성한다.
- 대안: Swing 링크를 직접 만들면 기본 간격·화면 이동·현재 설정 창의 상태 처리를 별도로 맞춰야 하므로 제외한다.
