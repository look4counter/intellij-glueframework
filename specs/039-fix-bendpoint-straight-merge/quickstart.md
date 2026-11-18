# 검증 안내

로컬 JBR 21·Gradle 9.7.1·기존 오프라인 초기화 설정으로 ModelDiagramTest와 Wire 편집·스냅·클립보드 회귀를 실행한다. 마지막 점을 초기 접점에서 5px보다 멀리 이동해 가로·세로 직선을 만들고 점 제거 및 현재 접점 유지, 저장·실행 취소를 확인한다.

## 실행 결과 — 2026-11-18

- 명확화 답변과 추가 재현 설명을 명세에 반영했다. 품질 점검 8개 통과, 구현 전 분석에서 요구사항 3개·성공 기준 2개가 작업에 연결됐으며 충돌·누락·미연결 작업은 없었다.
- 수정 전 신규 가로·세로 기하 재현에서 마지막 bendpoint가 남는 실패를 확인했다. 로그: build/bendpoint-merge-before.log. 최초 UI 재현은 겹친 다른 Wire를 선택하여 대상 Wire 인덱스를 명시적으로 검증하도록 테스트를 보완했다.
- 마지막 Activity 점 제거 후보에 현재 포인터를 직결 기준으로 전달했다. 명시 접점 플래그를 해제하고 기존 직결 외곽 계산을 사용한다. 임계치 확대나 다른 점의 자동 삭제는 하지 않는다.
- 이전 접점으로부터 6px 떨어졌다는 이유만으로 점을 유지하던 기존 기대와 스냅된 직선에도 점이 남는 기대를 현재 유효 직선 후보 기준으로 갱신했다. 유효 범위 밖 이동, 복원 및 관통 방지는 유지한다.
- 최종 테스트 143개가 실패·오류·건너뜀 없이 통과했다. ModelDiagramTest 75개, ModelDiagramClipboardTest 25개, ModelDiagramWireCreationTest 9개, ModelEditorFeaturesTest 16개, OrthogonalWireSnapTest 8개, ModelActivityCreationTest 10개다. 로그: build/bendpoint-merge-final.log.
- 새 위치의 가로·세로 병합, 미리보기, XML 재파싱, 사용자 속성 보존, 단일 실행 취소·다시 실행 및 실제 문서 저장을 검증했다.

```powershell
$env:GRADLE_USER_HOME = "$PWD/.gradle-user-home"
$bendJbr = "$PWD/.gradle-user-home/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr"
$env:JAVA_HOME = $bendJbr
& .gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.ModelDiagramTest --tests com.look4counter.ModelDiagramClipboardTest --tests com.look4counter.ModelDiagramWireCreationTest --tests com.look4counter.ModelEditorFeaturesTest --tests com.look4counter.OrthogonalWireSnapTest --tests com.look4counter.ModelActivityCreationTest --offline --no-configuration-cache -I .gradle-validation/press-position.init.gradle "-Porg.gradle.java.installations.paths=$bendJbr"
```

## 수렴 점검

요구사항 FR-001~003, 성공 기준 SC-001~002, 인수 시나리오 4개와 계획의 현재 접점 후보·기존 기하 보호·XML 저장 재사용 결정을 현재 코드 및 테스트와 대조했다. 누락·부분 구현·모순·미요청 변경은 0건으로 수렴 작업을 추가하지 않는다. 헌장은 미작성 템플릿이므로 규범 검사는 생략했다. 실제 사용자 IDE의 수동 조작과 배포 ZIP 재생성은 수행하지 않았다.
