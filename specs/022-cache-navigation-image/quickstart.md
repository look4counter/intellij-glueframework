# 검증 안내

기존 Gradle 환경에서 ModelDiagramPerformanceTest, ModelDiagramMiniMapTest, ModelDiagramZoomTest를 실행한다. IDE에서 우클릭으로 작은 이동과 화면보다 큰 이동을 수행하고 종료 후 글자 선명도와 미니맵 위치를 확인한다. 실제 체감 성능은 PC에서 비교해야 한다.

## 실행 결과

2026-11-06: 성능 12개, 미니맵 11개, 확대 4개로 총 27개 테스트가 통과했다. buildPlugin도 성공했다.

검증은 로컬 Gradle 9.7.1과 기존 오프라인 의존성 저장소, WebStorm의 JBR 21을 사용했다. 명령:

```powershell
.\.gradle-validation\gradle-9.7.1\bin\gradle.bat --offline --gradle-user-home .gradle-user-home --no-configuration-cache '-Porg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I .gradle-validation/ghost-offline.init.gradle test --tests '*ModelDiagramPerformanceTest' --tests '*ModelDiagramMiniMapTest' --tests '*ModelDiagramZoomTest' buildPlugin
```

연속 이동 중 노드 재렌더링 0회, 범위 밖 렌더링 보충, 종료 후 정상 화면과 픽셀 일치, 외형 변경 무효화 및 취소 복귀를 확인했다. 실제 IDE에서 체감 속도와 시작 시 지연은 별도 확인이 필요하다.

수렴 점검: 기능 요구사항 5개, 성공 기준 3개, 사용자 시나리오 3개 및 계획의 이미지 수명·메모리 제한·미니맵 재사용을 확인했다. 누락·부분 구현·충돌·미요청 변경은 없으며 추가 작업은 없다. 헌장은 미작성 템플릿으로 규범 검사를 생략했다.
