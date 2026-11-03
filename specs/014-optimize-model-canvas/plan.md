# 구현 계획: 대규모 모델 화면 최적화

**작성일**: 2026-10-27 | **명세**: [spec.md](spec.md)

## 요약과 기술 맥락

Kotlin·Swing·IntelliJ Platform 2025.3.6.1 기반 Canvas의 계산과 렌더링을 최적화한다. 모델 파일과 저장 계약은 유지한다. 기존 변경을 보존하며 승인된 세 단계를 순서대로 구현한다. 자동 검증은 JUnit 플랫폼 테스트와 합성 모델 측정으로 한다.

## 헌법 검사

헌법은 미작성 템플릿이다. 한글 문서와 speckit 절차를 준수한다. 설계 전후 위반 없음. 새 외부 라이브러리와 백그라운드 스레드는 추가하지 않는다. Swing 상태는 기존 UI 스레드에서 관리한다.

## 구조와 설계

### 1단계: 미니맵 계산·이미지 캐시

ModelDiagramCanvas.kt에서 모델 범위와 미니맵 projection을 캐시한다. DiagramMiniMap.kt에 사전 계산된 모델 범위를 받는 layout 경로를 추가하되 기존 layout 계약을 유지한다. 미니맵 배경 모델 이미지는 화면 좌표의 작은 BufferedImage로 보관하고 현재 뷰포트 사각형은 별도 표시한다. 팬·줌·스크롤·크기·폰트·배경·전경·색상 제공자·변경 강조를 캐시 키 또는 명시적 무효화로 처리한다. ModelDesignEditorProvider.kt의 설정 변경 경로에서 스타일 무효화를 호출한다. 장치 배율도 이미지 키에 포함한다. 모델 교체와 에디터 제거에서 오래된 캐시를 정리한다.

### 2단계: 표시 영역 필터링

렌더링 직전에 Graphics clip을 모델 좌표로 사용한다. Child 모양과 Wire 경로·라벨의 경계를 이용해 본 화면의 그리기를 제한한다. 선 굵기·화살표·선택 핸들의 여유를 포함한다. 라벨은 경로와 독립적으로 판단하여 경로가 화면 밖이어도 라벨이 표시된다. 선택·생성·삭제 강조와 순서를 유지한다. 미니맵 이미지를 갱신할 때도 동일한 경계 판단을 재사용한다.

### 3단계: 드래그의 부분 갱신

드래그가 임계치를 넘을 때 원본의 선택 노드 위치와 연결 Wire 위치를 한 번 인덱싱한다. 각 프레임은 선택 Child와 연결 Wire만 새 객체로 바꾸며 나머지는 참조를 재사용한다. 노드·Wire 목록의 순서는 유지한다. Wire 경로는 현재 객체와 끝점 미리보기 상태를 기준으로 캐시하고 변경된 슬롯만 갱신한다. 단일 Wire 편집도 같은 부분 갱신 경로를 사용한다.

DiagramBoundsIndex.kt는 좌표별 개수를 정렬 맵으로 유지하여 부분 변경 시 전체 범위를 다시 훑지 않도록 한다. 미니맵 이미지의 투영이 같으면 변경 전후 경계만 지우고 그 영역에 겹친 요소를 다시 그린다. 범위나 배율 변화로 투영이 달라지면 이미지를 다시 만든다. 외부 모델 로드·취소·저장은 안전한 전체 갱신 경로를 사용할 수 있다.

## 파일 범위

- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramMiniMap.kt
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramBoundsIndex.kt
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt
- src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt
- 기존 ModelDiagramTest·ModelEditorFeaturesTest·ModelDiagramAlignmentTest·ModelEditorRefreshTest 등 관련 회귀

## 검증 전략

합성 2,000개 Child 모델에서 캐시 준비 후 그리기 대상 수를 색상 제공자 호출로 관찰한다. 경로와 Wire 참조 재사용, 미니맵 반복 조회, 화면 경계의 Wire·라벨, 스타일 변경과 취소를 검사한다. 시간 수치는 500개·2,000개 모델의 반복 그리기·미니맵 조회·연속 드래그를 수정 전후 같은 테스트로 측정한다. 시간 자체는 성공/실패 경계로 사용하지 않는다. 기존 기능 검증과 설치용 ZIP 생성을 수행한다. 실제 사용자 모델의 체감 성능은 별도 수동 검증 항목으로 명시한다.
