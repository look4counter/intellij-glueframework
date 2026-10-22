# 파일 형식 계약

| 파일 | 형식 이름 | 담당 모듈 | 편집 방법 |
| --- | --- | --- | --- |
| example.glue_sql | Glue SQL | queryeditor | Design / XML Source |
| example.glue_uml_ad | Glue UML AD | modeleditor | Design / XML Source |

확장자는 점을 제외하여 각 모듈의 XML에 등록한다. 다른 확장자를 등록하지 않는다. 파일 내용은 텍스트 그대로 유지하고 기존 플러그인 ID도 유지한다.

탭 이름은 정확히 Design, Source이고 순서도 동일하다. 최초에는 Design이 선택되고 이후 선택은 IDE의 상태 복원을 따른다. 쿼리 Design은 [쿼리 편집 계약](query-design.md)의 목록·속성·SQL 편집을 제공하며 모델 Design은 기본 안내 화면을 유지한다. Source는 XML 언어와 IDE 문서를 사용하여 구문 강조·검사·완성·서식을 지원한다. 전용 스키마는 제공하지 않으며 구문 오류가 있는 문서도 열 수 있다. 제공자 식별자는 glue-query-design, glue-query-source, glue-model-design, glue-model-source로 구분한다.
