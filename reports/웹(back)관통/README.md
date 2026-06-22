# 부동산 실거래 정보 조회 서비스

![](../알고리즘%20관통/images/main.png)

주택 거래 정보를 조회하고 회원 관리와 관심지역 기능을 제공하는 서비스입니다.

## 프로젝트 구조

```text
pjt05_buk03_15_12
├─ backend/
│  ├─ src/main/java/com/ssafy/exam/
│  │  ├─ controller/
│  │  │  ├─ AuthController.java
│  │  │  ├─ FavoriteController.java
│  │  │  ├─ MemberController.java
│  │  │  ├─ PropertyController.java
│  │  │  └─ RegionController.java
│  │  ├─ filter/
│  │  │  └─ LoggingFilter.java
│  │  ├─ listener/
│  │  │  └─ MyContextListener.java
│  │  ├─ model/
│  │  │  ├─ dao/
│  │  │  ├─ dto/
│  │  │  └─ service/
│  │  └─ util/
│  └─ src/main/webapp/
│     ├─ index.jsp
│     ├─ css/
│     ├─ js/
│     ├─ clients/
│     ├─ error/
│     └─ WEB-INF/
```

- 백엔드는 MVC 아키텍처를 기반으로 `Controller - Service - DAO - DTO` 구조로 구성했습니다.

## 기능

### F501. 주택 실거래가 정보 수집 및 조회

![](../알고리즘%20관통/images/F501_데이터조회.png)

### F502. 주택 실거래가 정보를 원하는 검색

![](../알고리즘%20관통/images/F502_검색기능.png)

### F512. 회원 가입

![](../알고리즘%20관통/images/F512_회원가입.png)

### F513. 회원 정보를 조회하는 기능

![](../알고리즘%20관통/images/F513_회원조회1.png)
![](../알고리즘%20관통/images/F513_회원조회2.png)

### F514. 회원 정보를 수정하는 기능

![](../알고리즘%20관통/images/F514_회원수정1.png)
![](../알고리즘%20관통/images/F514_회원수정2.png)
![](../알고리즘%20관통/images/F514_회원수정3.png)

### F515. 회원 정보를 삭제하는 기능

![](../알고리즘%20관통/images/F515_회원삭제.png)
![](../알고리즘%20관통/images/F515_회원삭제2.png)

### F516, F503. 로그인, 로그아웃 / 관심지역

![](../알고리즘%20관통/images/F503_516_로그인_관심지역1.png)
![](../알고리즘%20관통/images/F503_516_로그인_관심지역2.png)

---

UseCase Diagram과 Class Diagram은 첨부한 Word 파일을 참고해주세요.
