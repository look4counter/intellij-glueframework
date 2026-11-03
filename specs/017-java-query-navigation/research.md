# 조사와 결정

기존 QueryNavigation.find는 commitAllDocuments를 호출하므로 읽기 액션에서 실행되는 참조 해석에 사용할 수 없다. QueryIdIndexService.snapshot은 읽기 가능한 불변 스냅샷으로 정확 일치 파일을 좁히는 데 재사용한다.

로컬 IntelliJ SDK의 javap로 PsiPolyVariantReferenceBase의 범위·소프트 참조 생성자, ElementManipulators.getValueTextRange, GotoDeclarationAction.findAllTargetElements를 확인했다. 기본 자바 문자열 참조 확장으로 Ctrl+클릭과 다중 후보를 제공한다. 문자열 비교는 PsiLiteralExpression.value를 사용하여 자바 이스케이프를 해석한다.

선언은 XML id 속성 값으로 반환한다. 이 위치는 query 태그 내부에 있어 기존 NavigatableFileEditor의 오프셋 이동 계약으로 쿼리 선택이 가능하다. 값이 일치하지 않거나 선언이 변경·삭제된 경우 후보를 제공하지 않는다. 소프트 참조이므로 일반 자바 문자열에 미해결 쿼리 경고를 추가하지 않는다.
