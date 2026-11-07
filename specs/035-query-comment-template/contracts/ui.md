# 사용자 인터페이스 계약

- Settings > GlueQueryEditor > Tool의 주석 템플릿 입력 이름은 query.comment.template이다. 여러 줄 입력과 지원 변수 안내를 제공한다.
- 주석 템플릿 아래에는 `${QUERY_ID}`는 현재 쿼리 ID, `${QUERY_DESCRIPTION}`는 현재 쿼리 설명, `${QUERY_RESULT_TYPE}`는 현재 쿼리 ResultType, `${yyyy-MM-dd HH:mm}`는 현재 날짜와 시간이라는 안내를 표시한다.
- 기본 템플릿 첫 내용 행은 `QueryId : ${QUERY_ID} / ${QUERY_DESCRIPTION}`이다. 작성자·수정이력·날짜와 들여쓰기는 유지하며 기존 저장 템플릿은 덮어쓰지 않는다.
- 주석 추가 버튼 이름은 query.action.comment이며 아래로 버튼 바로 다음에 배치한다. 툴팁과 접근성 이름은 주석 추가이다.
- QUERY_ID와 QUERY_DESCRIPTION은 달러/샤프 중괄호 문법 모두 지원한다. 날짜·시간은 y, M, d, H, h, m, s, S, a와 구분자로 된 유효한 형식이다.
- 빈 템플릿은 변경 없음. 그 외에는 정규화한 템플릿과 필요시 줄바꿈 하나를 기존 SQL 앞에 추가한다. 반복 삽입을 허용한다.
- 읽기 전용·비정상 XML·선택 없음·SQL 편집 불가 상태에서는 버튼이 비활성화된다. 지연 실행 중 선택이 달라지면 취소한다.

## 설정 트리

- GlueFramework > GlueQueryEditor > Tool: SQL 포매팅 및 주석 템플릿.
- GlueFramework > GlueQueryEditor > Database: 데이터소스, JDBC, 연결 테스트 및 메타데이터 도구.
- GlueQueryEditor 상위 페이지에는 항목 안내만 제공한다. 자식 ID는 기존 상위 ID 뒤의 .tool 및 .database이다.
- 두 하위 화면의 적용·초기화·변경 감지는 독립적이며 저장 서비스와 기존 값은 유지한다.
- GlueFramework 상위 화면은 IntelliJ 기본 그룹 화면으로 상단 설명과 GlueQueryEditor·GlueModelEditor 이동 링크를 표시한다. 자식 설정 ID와 Tool·Database 계층은 유지한다.
- GlueQueryEditor도 기본 그룹 화면으로 설명과 Tool·Database 링크를 제공한다. ResultType 변수는 선택한 쿼리의 최신 값을 사용하고 빈 값은 빈 문자열로 치환한다.
