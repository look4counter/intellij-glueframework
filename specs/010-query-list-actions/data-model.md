# 데이터 모델

새 Query 입력은 ID·Description·ResultType 문자열과 IsNamed 불리언이다. ID는 trim 후 비어 있지 않고 같은 목록에서 고유해야 한다. XML에서는 기존 id·desc·resultType·isNamed 속성을 사용한다. 복제본은 기존 태그 전체와 새 고유 ID를 가진다. 구조 명령 결과의 스마트 포인터로 추가·복제·이동 뒤 선택을 유지한다.
