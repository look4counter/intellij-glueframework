# 데이터 모델

영속 필드 추가는 없다. Activity↔Activity의 마지막 bendpoint 제거 후보만 firstRelativePoint를 현재 드래그 좌표로 바꾼다. 후보를 수락하면 기존 XML 경로가 새 기준과 점 삭제를 함께 저장한다. 거부하면 원본 기준 이동 결과를 유지한다.
