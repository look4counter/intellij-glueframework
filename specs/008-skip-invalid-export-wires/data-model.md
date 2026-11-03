# 데이터 모델

기존 ModelDiagram·DiagramWire·DiagramNode를 유지한다. Wire 참조는 선언 인덱스로 계산하고 원본 값과 번호를 변경하지 않는다. 변환 중 방향별 참조 횟수와 유효 인덱스를 임시로 수집한다. Service의 initial은 유효 시작점 이름 또는 빈 문자열이다.
