# 💬 BOOGLE 프로젝트 소개
----
BOOGLE은 외부 도서 데이터를 수집해 Elasticsearch 기반으로 빠른 검색 기능을 제공하는 도서 검색 서비스입니다.

도서 데이터를 효율적으로 검색하기 위해 Elasticsearch를 도입하고, 
Spring Batch를 활용해 외부 API 데이터 수집, 내부 DB 데이터 인덱싱 파이프라인을 구축했습니다.

# ⏰ 개발 기간
----
2026.02 ~ 2026.03


# 🐣 구성원 및 역할
----
⭐ 김수정
- JWT 기반 인증/인가 시스템 구현 및 공통 로깅 처리
- TypeScript 기반 프론트 화면 개발

⭐ 박현아
- Elasticsearch 기반 도서 검색 기능 구현
- Spring Batch를 활용한 DB ↔ Elasticsearch 데이터 동기화 파이프라인 구축
- AWS 기반 프론트엔드 인프라 구축 및 배포

⭐ 백가영
- Elasticsearch 기반 검색 기능 구현
- Next.js 기반 프론트엔드 개발

⭐ 한다은
- 외부 API → DB 데이터 수집 파이프라인 구축 (Spring Batch)
- App/Batch 멀티 모듈 구조 분리 설계
- AWS 기반 백엔드 인프라 구축 및 배포

# ⚙ 개발 환경
----
<div align=center><h1>📚 STACKS</h1></div>

<div align=center>
  
<!-- Frontend -->

<img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white"> <img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white"> <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black"> <img src="https://img.shields.io/badge/CSS-663399?style=for-the-badge&logo=css&logoColor=white">
<br>

<!-- Backend -->

<img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white"> <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"> <img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/Spring Data JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Spring Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/QueryDSL-0769AD?style=for-the-badge&logo=jquery&logoColor=white"> <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"> <img src="https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white">
<br>

<!-- Database --> <img src="https://img.shields.io/badge/PostgreSQL-336791?style=for-the-badge&logo=postgresql&logoColor=white"> <br> <!-- Infra / DevOps -->

<img src="https://img.shields.io/badge/AWS EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white"> <img src="https://img.shields.io/badge/AWS RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white"> <img src="https://img.shields.io/badge/AWS ECR-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/AWS CloudFront-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/AWS ACM-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white"> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/GitHub Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
<br>

<!-- Test --> <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white"> 
<br> 
</div>

# 🧩 데이터 분석 설계
----
ERD CLOUD
<img width="1428" height="660" alt="{17641EFD-869A-4D51-9526-11C560D4BD26}" src="https://github.com/user-attachments/assets/9f3bbfdc-de65-4322-9fdd-e3fde450f81c" />
