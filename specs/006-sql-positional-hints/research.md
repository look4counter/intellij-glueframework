# 조사 결과

- 결정: 기존 SqlLexer의 PARAMETER 토큰과 색상 팔레트를 재사용한다.
- 근거: 문자열·주석 제외와 밝은/어두운 테마 처리를 일치시킬 수 있다.
- 결정: 설치된 IntelliJ API의 ParameterHintsPresentationManager.addHint와 InlayModel을 사용한다.
- 근거: SQL 편집기가 PlainTextFileType의 EditorTextField라 일반 언어 PSI 힌트 제공자를 등록하는 것보다 직접 연결이 적합하다. 로컬 플랫폼의 실제 클래스 시그니처를 javap로 확인했다.
- 제외한 대안: 직접 텍스트를 문서에 삽입하면 SQL과 저장 내용이 변하므로 사용하지 않는다. 사용자 정의 렌더러 대신 기본 파라미터 힌트 렌더러를 사용한다.
- 문서 변경 시 lexer로 전체 SQL을 다시 훑고 힌트를 동기화한다. 쿼리별 버퍼가 작고 번호가 뒤쪽 전체에 영향을 주므로 우선 정확한 전체 갱신을 선택한다.
- 미해결 기술 질문은 없다.

