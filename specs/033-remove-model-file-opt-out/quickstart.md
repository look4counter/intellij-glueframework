# 검증 안내

GlueModelEditor 설정에서 옵션이 사라졌는지 확인한다. 과거 true 설정 XML을 로드한 뒤 유효 값과 다시 저장한 값이 false인지 확인하고 다른 설정이 유지되는지 확인한다. Open Service가 모델 파일을 선택하고 서비스 XML이 직접 Design provider를 받지 않는지 검증한다.

관련 설정·서비스·모델 회귀 후 전체 test·buildPlugin·verifyPluginStructure를 실행한다. 기존 로컬 JBR·Gradle 캐시를 사용하고 IDE 소켓 제약으로 검색 옵션 생성을 제외한다. 실제 사용자 IDE 수동 확인과 자동 검증을 구분한다.

## 자동 검증 결과: 2026-11-08

- 설정·서비스 어댑터·일반 모델 내보내기·편집기 열기 집중 검증이 통과했다. 체크박스 부재, 이전 true XML의 false 정규화와 재저장, 상태 사본 변경 방어, 다른 설정 보존을 확인했다. 서비스 XML 직접 편집 provider 제외와 일반 모델 파일 선택도 검증했다.
- 최종 전체 테스트 512개가 통과했다. 실패·오류·건너뜀은 각각 0개다. 폐기한 true 모드에서만 실행 가능한 UI 시나리오 6개는 제거했고, 독립 서비스 어댑터와 일반 모델 기능 검증은 유지했다. 좌표 이동 검증 1개 추가로 이전 517개에서 512개가 됐다.
- 전체 회귀 중 좌표의 예전 좌측 상단 위치를 가정한 테스트를 발견했다. 연결선 라벨 검증은 유지하고 좌표 검증은 배경 도형의 영향을 받지 않는 새 미니맵 테스트로 분리했다. 수정 후 전체 회귀가 통과했다.
- `test buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. 기존 IDE 소켓 제약으로 검색 옵션 생성만 제외했다. 설치 ZIP과 SHA-256을 갱신했다. 실제 사용자 IDE 수동 조작은 수행하지 않았다.
- 근거: `.gradle-validation/model-option-removal-focused.log`, `.gradle-validation/model-option-removal-final.log`, `build/reports/model-option-removal-test-summary.json`.

## 수렴 검토

요구사항 4개·성공 기준 3개·사용자 이야기 1개와 계획의 상태 정규화·설정 UI·회귀 유지·문서 및 패키징 결정을 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 각각 0개다. 헌장은 미작성 템플릿으로 규범 검사를 생략했고 확장 훅은 없다. 수렴 중 추가할 작업이 없어 작업 목록은 변경하지 않았으며, 검토 후 구현 단계의 완료 기록만 갱신했다.
