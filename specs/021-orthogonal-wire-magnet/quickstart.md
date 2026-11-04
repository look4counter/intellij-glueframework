# 검증 안내

1. JDK 21에서 `./gradlew.bat test --tests com.look4counter.OrthogonalWireSnapTest --tests com.look4counter.ModelSnapSettingsTest --tests "com.look4counter.ModelDiagram*" --tests com.look4counter.ModelActivityCreationTest --tests com.look4counter.ModelEditorCreationTraceTest --tests com.look4counter.ModelEditorFeaturesTest --tests com.look4counter.ChildCreationToolbarTest`를 실행한다.
2. `./gradlew.bat buildPlugin`으로 설치용 ZIP을 생성한다.
3. Snap 옆 Orthogonal 버튼을 켜고 Wire 꺾임점과 중앙 핸들을 드래그한다. 인접 구간과 직각이 되는 위치 근처에서 정확히 붙는지 확인한다.
4. 붙은 상태로 약간 움직이면 유지되고 더 멀리 움직이면 풀려야 한다. 줌 변경에도 화면상 감도가 같아야 한다.
5. Alt 우회와 버튼 해제, 다른 편집기의 설정 공유, 취소, 저장, 실행 취소 및 다시 실행을 확인한다.

## 실행 결과 — 2026-11-04

- OrthogonalWireSnapTest 신규 8개를 포함하여 총 199개 모델 편집 회귀 테스트 통과, 실패 0개.
- 토글 순서, 기본 꺼짐, Snap과 독립된 상태, 여러 편집기 공유 및 직렬화 복원을 검증했다.
- 6px 진입·10px 유지 및 이탈, 축별 보정, 확대율 20%·50%·100%·150%, 음수 좌표와 기존 Snap 결과의 독립 적용을 검증했다.
- 실제 꺾임점 및 중앙 핸들 드래그, Activity 접점 재계산 후 직각 유지, Alt·토글 해제의 즉시 반영, 취소 및 다음 제스처 상태 초기화를 검증했다.
- 미리보기와 확정 좌표 일치, Wire 사용자 속성 및 가중치 보존, 저장·실행 취소·다시 실행을 확인했다.
- buildPlugin 성공. 설치 산출물: build/distributions/intellij-glueframework-1.0.1.zip.
- 환경: JDK 21.0.6 및 작업 폴더 내 Gradle 9.7.1, 기존 .gradle-validation/ghost-offline.init.gradle을 사용한 오프라인 검증. 프로젝트 의존성 설정 변경 없음.
