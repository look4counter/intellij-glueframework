# 포매팅 계약

쿼리 시작 열을 B라고 하면 SELECT는 B에서 시작하고 T 기준 열은 B+5다. FROM·WHERE·AND·OR·HAVING과 GROUP BY·ORDER BY의 첫 단어 마지막 글자를 그 열에 맞춘다. SELECT 후속 목록은 B+5에서 콤마와 공백을 쓰며 CTE 후속 정의는 B에서 콤마를 쓴다. GROUP BY·ORDER BY 후속 목록의 표현식은 첫 항목의 열을 따른다.

CTE·SELECT 서브쿼리 본문은 4칸 들여쓴다. CASE의 WHEN·THEN·ELSE·END는 해당 CASE 시작 열에 맞춘다. 함수 인자 콤마는 인라인이다. 주석·문자열·인용 식별자·파라미터 본문은 보존하고 줄 주석 뒤에는 줄바꿈한다. 포맷은 멱등적이어야 한다.

현재 IDE 코드 정리 단축키는 선택한 쿼리 전체에만 적용한다. 읽기 전용·잘못된 XML·혼합 본문·미선택·폐기·지연 중 선택 변경은 수정하지 않는다. 닫히지 않은 인용·블록 주석·괄호, 한계 초과는 오류 안내 후 원문을 보존한다. 포맷할 변경이 없으면 새 Undo 항목을 만들지 않는다.

UPDATE·INSERT의 기준 열은 각 E·T이며 SET·INSERT 컬럼·VALUES 목록도 앞쪽 콤마를 사용한다. MERGE는 긴 UPDATE 절까지 공통 열에 맞추기 위해 첫 MERGE를 한 칸 들여써 E를 공통 기준으로 사용한다. SQL 포맷 버튼은 제거한다.

최신 FR-013~015의 사용자 예제가 이전 서브쿼리·INSERT·MERGE 배치를 대체한다. 서브쿼리는 여는 괄호 열+4에 SELECT, 닫는 괄호는 여는 열이다. INSERT INTO는 한 줄이며 컬럼·값은 6칸, 앞쪽 콤마는 4칸에 둔다. 일반 INSERT의 컬럼 닫는 괄호와 VALUES는 같은 줄이다. MERGE INTO와 USING은 시작 열이며 ON은 USING 뒤, WHEN MATCHED THEN·WHEN NOT MATCHED THEN은 각각 한 줄이다. MERGE 내부 INSERT 목록과 VALUES 목록은 별도 닫는 괄호 줄을 사용하며 VALUES 다음 여는 괄호도 별도 줄이다.

PIVOT·UNPIVOT의 본문은 키워드 시작 열+4, IN 항목은 본문 열+6, 앞쪽 콤마는 항목 열-2다. 바깥 닫는 괄호는 키워드 열, IN 닫는 괄호는 본문 열이다. 함수 인자와 UNPIVOT 튜플은 인라인으로 유지한다. FROM의 구분자 없는 인접 서브쿼리도 토큰을 추가하지 않고 예제처럼 같은 표현식 열에 줄바꿈한다.
