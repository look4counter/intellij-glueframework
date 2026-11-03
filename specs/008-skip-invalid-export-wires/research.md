# 조사 결과

## 결정

생성 저장 경로는 ModelServiceSaveListener.save → ModelServiceXmlExporter.export다. exporter에서 비정상 Wire의 전체 예외를 제거하고 유효 Wire로만 초기 연결과 전이를 만든다.

## 근거

기존 파서는 잘못된 형식의 참조를 무시하지만 유효 숫자의 범위는 제한하지 않는다. 따라서 원본 outputs로 직접 배열을 참조하지 않고 유효 인덱스 집합을 거쳐야 한다. 여러 노드의 참조 및 같은 노드의 중복도 참조 횟수로 판별한다.

Writer는 service의 name·initial 속성을 필수로 요구한다. ServiceDiagramAdapter는 빈 initial을 허용하므로 시작 연결이 없을 때 빈 값으로 저장한다. 직접 Service 편집의 Commit은 별도 경로이므로 수정하지 않는다.

## 대안

- 저장 예외를 통째로 무시: 정상 출력이 갱신되지 않으므로 제외.
- 원본 Wire 삭제: 사용자 모델을 바꾸므로 제외.
- 잘못된 인덱스를 제거한 뒤 재번호 부여: 정상 연결 대상을 바꿀 수 있어 제외.
