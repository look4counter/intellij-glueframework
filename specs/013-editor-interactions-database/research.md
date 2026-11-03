# 조사 결과

## 드래그

결정: 운영체제 화면 좌표 임계치를 적용한다. 기존 press의 onMoveStarted를 최초 유효 drag로 옮긴다. 모델 좌표 기준은 줌에 따라 손떨림 허용 거리가 달라지므로 제외한다.

## 괄호

결정: 기존 SQL 렉서와 내장 편집기의 markup을 사용한다. 현재 편집기는 PlainText 파일 타입에 사용자 SQL highlighter만 부착하므로 언어 전역 확장을 추가하지 않는다. 문서 변경과 커서 이동 및 폐기를 함께 처리한다.

## Oracle

결정: 객체 목록과 컬럼을 각각 일괄 조회한다. 현재 getTables 이후 각 테이블마다 getColumns를 호출하는 구조이므로 왕복 횟수가 테이블 수에 비례한다. 테이블만 수집하면 기존 뷰 자동완성이 사라지므로 ALL_VIEWS도 유지한다.

근거: [Oracle ALL_TAB_COLUMNS](https://docs.oracle.com/en/database/oracle/oracle-database/12.2/refrn/ALL_TAB_COLUMNS.html)는 접근 가능한 테이블·뷰의 컬럼과 OWNER·TABLE_NAME·COLUMN_ID를 제공한다. 전체 스키마 범위를 유지한다. 실제 속도 수치는 서버 환경에서 별도 측정해야 한다.

## 연결 테스트

결정: 메타데이터를 읽지 않고 Driver.connect 성공을 확인한다. 기존 격리 로더·타임아웃·자원 해제를 공통화한다. 비밀번호 저장은 수행하지 않는다. 접속 오류 메시지는 원문을 UI에 노출하지 않는다.

미해결 기술 항목은 없다.
