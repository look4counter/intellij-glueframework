# 데이터 모델

- QueryCommentState: template 문자열. 기본값은 사용자 예시이며 빈 문자열도 허용한다.
- QueryCommentSettings: 프로젝트 서비스. 상태 읽기·쓰기는 복사해 외부 변경을 차단한다.
- 치환 문맥: queryId, queryDescription, resultType, now. 읽기 전용이며 삽입할 때 최신 값으로 구성한다. resultType의 기본값은 빈 문자열이다.
- 상태 흐름: 저장값 → 설정 초안 → 적용 또는 초기화. SQL 삽입은 설정 초안을 읽지 않는다.
