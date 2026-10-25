# 데이터 모델

노드 크기가 바뀌었으면 중심 변화량을 구해 각 꺾임점의 해당 노드 기준 상대 좌표에서 뺀다.
반대편 벡터, weight, 순서와 개수는 보존한다. 양쪽이 Resize되면 양쪽 벡터를 각각 보정한다.
XML의 firstDimension과 secondDimension에 결과를 기록한다. 원래 태그와 추가 속성을 유지한다.
일반 노드 이동과 Activity 경계 접점 계산은 변경하지 않는다.

Canvas의 snapEnabled 하나로 Wire와 Child 정렬을 제어한다. 이 값은 전역 설정에서 초기화하며 모델 XML에는 저장하지 않는다.


## 전역 Snap 데이터

ModelSnapState.enabled의 기본값은 false다. 애플리케이션 설정에 저장하며 모델 XML에는 저장하지 않는다.
각 Canvas의 snapEnabled는 이 설정의 표시용 복제 값이고 모든 편집기가 동일 변경 이벤트를 구독한다.
서비스의 getState는 분리된 복사본을 반환하고 loadState도 열린 편집기에 변경을 알린다.

