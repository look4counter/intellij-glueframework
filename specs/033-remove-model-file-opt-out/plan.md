# 구현 계획: 모델 파일 사용 해제 옵션 제거

## 기술 맥락과 헌장

기존 Kotlin·Swing 설정 패널·ModelEditorSettings를 사용한다. 외부 의존성·문서 형식은 변경하지 않는다. 헌장은 미작성 템플릿이므로 규범 검사를 생략하며 한글 문서·speckit 순서를 준수한다.

## 구현 단계

1. ModelEditorSettings.kt의 loadState에서 이전 noUseGlueUmlAdFile 값을 false로 정규화한다. 호환 필드와 기본 false는 유지해 이전 XML을 읽고 정상 상태만 저장한다.
2. GlueModelEditorConfigurable.kt에서 체크박스 선언·배치·초안 저장·로드를 제거한다. 설정 초안은 모델 파일 사용을 해제하지 않는다.
3. ModelEditorSettingsTest.kt의 체크박스 선택 검증을 체크박스 부재·과거 true XML 로드·false 재저장·다른 설정 보존 검증으로 교체한다. ModelServiceDirectEditorTest.kt의 폐기 모드 전용 UI 시나리오는 일반 모드/provider 마이그레이션 검증으로 교체하고 독립된 서비스 어댑터 단위 검증은 유지한다. EditorOpenThreadTest.kt의 폐기 모드 검증도 이전 true가 서비스 provider를 활성화하지 않는지 확인하도록 바꾼다. 일반 모델 열기의 스레드 검증은 기존 별도 테스트를 유지한다.
4. README·quickstart와 기존 모델 명세에 폐기 사실을 기록한다. 집중 검증 후 전체 test·buildPlugin·verifyPluginStructure를 실행한다. 기존 IDE 소켓 제약으로 검색 옵션 생성을 제외하고 명시한다.

## 기존 동작에 미치는 영향

ServiceFiles·ServiceDiagramAdapter.accepts·ModelServiceSaveListener는 유효 설정을 false로 읽어 기존 false 분기를 따른다. 새로운 우회 설정이나 테스트용 활성화 경로는 추가하지 않는다. 원본 모델·서비스 파일은 변경하지 않는다.
