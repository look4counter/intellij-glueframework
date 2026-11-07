# 구현 계획: SQL 사용자 줄바꿈 보존

## 기술 맥락과 헌장

기존 Kotlin·SqlFormattingTokens·SqlFormatter와 IntelliJ 편집 동작을 사용한다. 외부 의존성은 추가하지 않는다. 헌장은 미작성 템플릿으로 검사를 생략하며 한글 문서·speckit 절차를 준수한다.

## 구현 설계

1. `queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormatter.kt`의 Writer가 원문과 원본 소비 위치를 보관한다. 각 토큰 전에 원래 공백 경계를 읽어 실제 줄바꿈만 출력한다. 기존 line 호출은 행 시작이면 들여쓰기를 적용하고 같은 행이면 아무것도 추가하지 않는다. 일반 토큰 구분 공백은 spaceBefore와 키워드 분기에서 별도로 처리한다. 공백 지연으로 줄 끝 주석과 문서 끝에 불필요한 공백을 추가하지 않는다.
2. 복합 절 키워드는 원래 각 토큰 경계를 거쳐 출력한다. 재귀 괄호·CTE 본문·닫는 괄호·주석·CTE 구분자에도 원본 경계를 적용한다. 주석 전체 줄을 출력한 경우 이미 포함한 앞뒤 공백을 다시 소비하지 않는다.
3. Writer의 행·열은 원본 줄바꿈으로 갱신하므로 기존 등호·AS 정렬의 두 번 렌더링을 유지한다. 원문 끝의 줄바꿈과 빈 줄을 마지막에 복원한다. formatSelection의 기존 앞뒤 줄바꿈 덧붙이기는 제거한다.
4. `src/test/kotlin/com/look4counter/SqlFormatterTest.kt`에서 자동 행 생성을 기대하던 검증을 새 계약으로 갱신하고 원본 줄 경계·복합 키워드·빈 줄·LF/CRLF·주석·문자열·선택을 검증한다. `QuerySqlEditorTest.kt`의 전체·선택 포매팅·옵션·Undo/Redo 기대값도 갱신한다.
5. README와 quickstart를 갱신하고 집중 검증 후 전체 test·buildPlugin·verifyPluginStructure를 실행한다. 기존 IDE 소켓 제약으로 검색 옵션 재생성을 제외하고 명시한다.

## 복잡성과 제약

원문 경계를 토큰 오프셋으로 한 번씩 소비한다. 기존 입력 길이·토큰·깊이 제한과 취소 검사를 유지한다. 새 줄을 만드는 대체 옵션은 추가하지 않는다. 기존 포매팅 상태 저장·편집 명령 경로는 변경하지 않는다.

## 공백 대체 취소 보완

Writer.line의 같은 행 분기에서 space 호출을 제거한다. 절·CASE 분기·AND/OR와 인접 파생 테이블·MERGE VALUES의 토큰 구분은 명시적으로 처리해 SELECT와 FROM 등이 붙지 않도록 한다. 기존 단일 행 기대값은 일반 콤마와 괄호 공백 규칙으로 갱신하고 다중 행·정렬·주석·선택·Undo/Redo 검증은 유지한다.

## 인라인 서브쿼리 기준 열 보완

SqlFormatter.range에서 SELECT 출력 직전에 Writer의 실제 시작 열을 기록하고 이후 절·조건·PIVOT와 일반 괄호 재귀의 기준으로 사용한다. 여는 괄호 다음 줄을 가정한 초기 base를 인라인 SELECT에도 계속 사용하던 문제를 해소한다. 세미콜론에서는 초기 기준으로 복원한다. SqlFormatterTest.kt에 사용자 예제와 중첩·SELECT 다음 줄 배치·정렬 옵션 병행·반복 실행을 검증하고 QuerySqlEditorTest.kt에서 실제 명령 결과를 확인한다.
