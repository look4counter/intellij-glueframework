# 대규모 모델 화면 최적화 검증

## 재현과 자동 측정

ModelDiagramPerformanceTest는 동일한 500개·2,000개 Child 및 인접 Child 연결 Wire를 생성한다. 600×400 화면에서 3회 준비 후 7회 그리기 시간의 중앙값을 비교한다. 미니맵 조회는 100회 합계, 드래그는 화면 그리기를 제외한 마우스 이동 15회 처리의 합계다. 시간은 성공/실패 기준으로 사용하지 않는다.

2026-10-27 동일 로컬 환경의 측정 결과:

| Child 수 | 항목 | 수정 전 | 수정 후 |
| --- | --- | ---: | ---: |
| 500 | 반복 그리기 | 39.60ms | 2.05ms |
| 2,000 | 반복 그리기 | 62.09ms | 2.91ms |
| 500 | 미니맵 조회 100회 | 45.65ms | 0.08ms |
| 2,000 | 미니맵 조회 100회 | 62.42ms | 0.04ms |
| 500 | 드래그 이동 15회 처리 | 21.98ms | 1.24ms |
| 2,000 | 드래그 이동 15회 처리 | 22.92ms | 2.76ms |
| 500 | 준비 후 그리기당 Child 색상 조회 | 1,000회 | 24회 |
| 2,000 | 준비 후 그리기당 Child 색상 조회 | 4,000회 | 24회 |

수정 전 원본 측정은 build/reports/model-canvas-before.csv, 수정 후 원본 측정은 build/reports/model-canvas-after.csv다. 로그는 build/model-canvas-before.log와 build/model-canvas-verified.log다. 수정 전 미니맵 재사용 검사에서 실패를 재현했다. 합성 모델의 측정값이며 실제 사용자 모델과 하드웨어의 체감 시간은 다를 수 있다.

## 동작 검증

- 같은 상태의 미니맵 100회 조회에서 같은 투영을 재사용하고, 준비 후 반복 그리기에서 전체 미니맵을 다시 그리지 않는 것을 확인한다.
- 화면 밖 도형의 그리기를 건너뛰고 전체 그리기와 부분 클립의 픽셀이 일치하는 것을 확인한다. 양끝이 화면 밖인 Wire와 긴 라벨도 유지한다.
- 이동·크기 조절·Wire 편집 이후 부분 갱신한 미니맵과 모델 전체를 새로 그린 미니맵을 픽셀 단위로 비교한다. 취소 후에도 일치해야 한다.
- 무관한 Wire와 경로의 참조를 유지하며 연속 드래그가 원본 기준으로 계산되는 것을 확인한다.
- 색상 제공자 교체·명시적 스타일 무효화·폰트·강조·줌·팬·화면 크기·빈 모델 전환을 검사한다.
- 기존 생성·선택·스냅·정렬·드래그 임계치·읽기 전용·새로고침·실행 취소·저장 회귀를 실행한다.

## 실행 명령

```powershell
$env:GLUE_CANVAS_BENCHMARK='after'
& ./.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.ModelDiagramPerformanceTest --tests com.look4counter.ModelEditorFeaturesTest --tests com.look4counter.ModelDiagramTest --tests com.look4counter.ModelDiagramAlignmentTest --tests com.look4counter.ModelDiagramPropertiesTest --tests com.look4counter.ModelEditorRefreshTest --tests com.look4counter.ModelEditorSettingsTest --tests com.look4counter.ModelServiceDirectEditorTest --tests com.look4counter.ModelEditorServiceExportTest buildPlugin -x buildSearchableOptions --offline -g .gradle-user-home --no-daemon --no-configuration-cache '-Dorg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I build/state-wire-check/local-repo.init.gradle
```

수정 전 측정은 같은 측정 테스트의 testLargeModelMeasurement와 미니맵 재사용 회귀를 실행하고 GLUE_CANVAS_BENCHMARK를 before로 지정했다. 실제 사용자 IDE에서의 수동 조작은 수행하지 않았다.

## 설치용 산출물

build/distributions/intellij-glueframework-1.0.0-SNAPSHOT.zip을 생성했다. ZIP 안 JAR에 ModelDiagramCanvas·DiagramMiniMap·DiagramBoundsIndex 변경 클래스가 포함된 것을 확인했다. 기존 환경의 검색 옵션 수집용 IDE 실행 제한에 따라 검색 옵션 재생성은 제외하고 기존 산출물을 사용했다.

## 최종 회귀 및 수렴 결과

ModelDiagramPerformanceTest 7개, ModelEditorFeaturesTest 15개, ModelDiagramTest 73개, ModelDiagramAlignmentTest 14개, ModelDiagramPropertiesTest 29개, ModelEditorRefreshTest 12개, ModelEditorSettingsTest 9개, ModelEditorServiceExportTest 10개, ModelServiceDirectEditorTest 10개로 총 179개 고유 테스트가 통과했다. 전체 실행에서 다른 169개 검사는 통과했으며, 기존 Service 직접 편집 검증의 Commit 전후 기대값을 현재 저장 계약에 맞춘 후 해당 클래스 10개를 다시 실행하여 통과했다. 제품 코드는 전체 실행 이후 변경하지 않았다.

검증 로그는 build/model-canvas-verified.log와 build/model-canvas-service-verified.log, 사례별 결과는 build/model-canvas-validation.json이다. 직접 편집 검증에는 기존 Commit 전에는 Source가 유지되고 Commit 후에 변경되는 것과 실행 취소·재실행·연결 재투영을 포함한다. 저장 의미나 제품 동작은 수정하지 않았다.

최종 수렴에서 기능 요구사항 6개, 성공 기준 4개, 사용자 이야기 3개와 캐시·클립·부분 갱신 설계를 현재 코드 및 테스트와 대조했다. 보완 작업 T013까지 완료했으며 누락·부분 구현·상충·범위 밖 구현은 0개다. 헌법은 미작성 템플릿이므로 추가 원칙 검사를 생략했다.
