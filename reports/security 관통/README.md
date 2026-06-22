# 1. SSAFY HOME — 부동산 실거래 정보 조회 서비스

<img src="./images/screens/home.png" alt="SSAFY HOME 메인 화면" style="max-width:900px;" />

공공데이터 기반 주택 매매 실거래가를 MySQL 에 적재하고, 회원·관심지역·공지·게시판 기능과 함께 조회 서비스를 제공하는 SSAFY HOME 프로젝트입니다. 이번 회차에서는 기존 세션 직접 처리 방식의 인증·인가를 **Spring Security** 기반으로 전환하고, JDBC DAO 쿼리를 **MyBatis Mapper 인터페이스 + XML mapper** 로 옮겨 영속화 계층을 정리했습니다.

## 1.1. 프로젝트 구조

```text
pjt09_buk03_15_09
├─ README.md
├─ backend/
│  ├─ pom.xml                                        # Maven 설정 (spring-boot-starter-parent 3.5.x)
│  ├─ mvnw, mvnw.cmd                                 # Maven wrapper
│  ├─ src/main/
│  │  ├─ java/com/ssafy/prj/
│  │  │  ├─ PjtApplication.java                      # @SpringBootApplication 진입점
│  │  │  ├─ config/                                  # Spring 설정 클래스 (Security, MVC 등)
│  │  │  ├─ controller/                              # 요청 처리 컨트롤러
│  │  │  │  ├─ AuthController
│  │  │  │  ├─ MemberController
│  │  │  │  ├─ BoardController
│  │  │  │  ├─ NoticeController
│  │  │  │  ├─ HouseInfoController
│  │  │  │  ├─ CommercialController
│  │  │  │  ├─ FavoriteController
│  │  │  │  └─ ...
│  │  │  ├─ filter/                                  # 요청 필터 (CORS, 로깅 등)
│  │  │  ├─ model/
│  │  │  │  ├─ mapper/                               # MyBatis Mapper 인터페이스
│  │  │  │  │  ├─ UserAccountMapper
│  │  │  │  │  ├─ BoardMapper
│  │  │  │  │  ├─ NoticeMapper
│  │  │  │  │  ├─ HouseInfoMapper
│  │  │  │  │  ├─ CommercialMapper
│  │  │  │  │  ├─ FavoriteAreaMapper
│  │  │  │  │  ├─ DongCodeMapper
│  │  │  │  │  └─ RealEstateTransactionMapper
│  │  │  │  ├─ dto/                                  # 데이터 전달 객체
│  │  │  │  └─ service/                              # 비즈니스 로직 계층
│  │  │  └─ util/                                    # 유틸리티 클래스
│  │  ├─ resources/
│  │  │  ├─ application.properties                   # Spring Boot 설정
│  │  │  ├─ mappers/                                 # MyBatis XML mapper
│  │  │  │  ├─ UserAccountMapper.xml
│  │  │  │  ├─ BoardMapper.xml
│  │  │  │  ├─ NoticeMapper.xml
│  │  │  │  ├─ HouseInfoMapper.xml
│  │  │  │  ├─ CommercialMapper.xml
│  │  │  │  ├─ FavoriteAreaMapper.xml
│  │  │  │  ├─ DongCodeMapper.xml
│  │  │  │  └─ RealEstateTransactionMapper.xml
│  │  │  └─ static/
│  │  │     ├─ clients/                              # 클라이언트 앱 (HTML, JavaScript)
│  │  │     ├─ css/                                  # 스타일시트
│  │  │     └─ js/                                   # JavaScript 유틸리티
│  │  └─ webapp/WEB-INF/views/                       # JSP + JSTL 템플릿
│  └─ target/                                         # Maven 빌드 결과
└─ sql/
   ├─ SSAFY_HOME_Schema.sql                         # 스키마 및 테이블 DDL
   ├─ users_and_favorites.sql                       # 회원·관심지역 테이블 + 초기 데이터
   ├─ notices.sql                                   # 공지 테이블 + 초기 데이터
   ├─ boards.sql                                    # 게시판 테이블 + 초기 데이터
   ├─ commercials.sql                               # 상권정보 테이블
   └─ load_commercials_summary.sql                  # 상권정보 데이터 일괄 적재
```

- 백엔드는 `Controller - Service - Mapper - DTO` MVC 구조이며, DB 연동은 **MyBatis + MySQL** 로 처리합니다.
- MyBatis Mapper 인터페이스는 `model/mapper/` 에 정의되고, `resources/mappers/*.xml` 에 쿼리가 매핑됩니다.
- 인증·인가는 **Spring Security** 의 `SecurityFilterChain`, `UserDetailsService`, `@AuthenticationPrincipal` 로 처리합니다.
- Spring 빈은 생성자 주입(`@RequiredArgsConstructor` + `final` 필드) 으로 연결하고, 경로 기반 URL 매핑을 사용합니다.

## 1.2. 기술 스택

| 영역         | 기술                                                                                                      |
| ------------ | --------------------------------------------------------------------------------------------------------- |
| **Backend**  | Java 21, Spring Boot 3.5.x (`spring-boot-starter-web`, `spring-boot-starter-aop`, `spring-boot-devtools`) |
| **Security** | Spring Security, `SecurityFilterChain`, `UserDetailsService`, `CustomUserDetails`, Role 기반 인가         |
| **Database** | MySQL 8.x (`ssafy_home` 스키마), MyBatis Spring Boot Starter 3.0.5, XML mapper, `mysql-connector-j`       |
| **View**     | JSP + JSTL (`spring.mvc.view.prefix=/WEB-INF/views/`), Spring Security Taglibs, Bootstrap 5, Kakao Map SDK |
| **JSON**     | Jackson Databind (`spring-boot-starter-web` 내장)                                                         |
| **Build**    | Maven, Spring Boot Maven Plugin, 임베디드 Tomcat (`tomcat-embed-jasper` 로 JSP 처리)                      |
| **기타**     | Lombok, SHA-256 `PasswordEncoder`, Filter 기반 CORS, `@ExceptionHandler` 기반 에러 응답                  |

> 주요 변경점: 로그인 성공 후 세션 값을 직접 조회하던 흐름을 Spring Security 인증 객체와 `@AuthenticationPrincipal` 기반으로 바꾸고, `users`·`favorite_areas`·`notice`·`boards` 등 DB 접근은 `model/mapper` 인터페이스와 `resources/mappers/*.xml` 로 분리했습니다.

## 1.3. Spring Security 적용 세부 사항

### 1.3.1. SecurityFilterChain 설정

`config/CustomSecurityConfig.java` 에서 Spring Security 를 구성합니다:

```java
@Configuration
@EnableWebSecurity
public class CustomSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
            .requestMatchers("/auth/**", "/property/**", "/region/**", "/commercial/**", "/houseinfo/**", "/notice/**", "/board/**")
            .permitAll()
            .requestMatchers("/favorite/**", "/mypage", "/board/my")
            .authenticated()
            .requestMatchers("/admin/**", "/member/list", "/member/changeRole", "/notice/registerForm", "/notice/register")
            .hasRole("ADMIN")
            .anyRequest()
            .denyAll()
        )
        .formLogin(form -> form
            .loginPage("/auth/login")
            .loginProcessingUrl("/auth/login")
            .defaultSuccessUrl("/")
            .failureUrl("/auth/login?error=true")
        )
        .logout(logout -> logout
            .logoutUrl("/auth/logout")
            .logoutSuccessUrl("/")
        );
        
        return http.build();
    }
}
```

### 1.3.2. UserDetailsService 구현

`CustomUserDetailsService` 는 Mapper 를 통해 회원을 조회하고 Spring Security 에 전달합니다:

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserAccountMapper userAccountMapper;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userAccountMapper.findByUserId(username);
        if (user == null) throw new UsernameNotFoundException("User not found");
        
        return new CustomUserDetails(user);
    }
}
```

### 1.3.3. CustomUserDetails

회원 정보를 Spring Security 에 호환하는 형태로 제공:

```java
public class CustomUserDetails implements UserDetails {
    private UserAccount user;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }
    
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    
    @Override
    public String getUsername() {
        return user.getUserId();
    }
    
    // 다른 메서드들...
}
```

### 1.3.4. Controller 에서 인증된 사용자 접근

`@AuthenticationPrincipal` 로 현재 로그인한 사용자의 정보를 주입받습니다:

```java
@PostMapping("/favorite/add")
public ResponseEntity<ApiResponse<?>> addFavorite(
    @AuthenticationPrincipal CustomUserDetails userDetails,
    @RequestParam String sidoCode,
    @RequestParam String sigunguCode,
    @RequestParam String emdCode
) {
    String userId = userDetails.getUsername();
    FavoriteArea favoriteArea = new FavoriteArea(userId, sidoCode, sigunguCode, emdCode);
    
    int result = favoriteAreaService.addFavoriteArea(favoriteArea);
    return ResponseEntity.ok(new ApiResponse<>("success", "관심지역 등록 완료", result));
}
```

### 1.3.5. 권한 기반 접근 제어

URL 패턴 기반 + 메서드 레벨 권한 검증:

```java
@PostMapping("/member/changeRole")
public ResponseEntity<ApiResponse<?>> changeRole(
    @AuthenticationPrincipal CustomUserDetails userDetails,
    @RequestParam String userId,
    @RequestParam String newRole
) {
    // SecurityConfig 에서 이미 ROLE_ADMIN 만 접근 가능
    // 추가적으로 자신은 강등할 수 없도록 검증
    if (userDetails.getUsername().equals(userId)) {
        return ResponseEntity.badRequest()
            .body(new ApiResponse<>("error", "자신의 권한을 변경할 수 없습니다", null));
    }
    
    memberService.updateRole(userId, newRole);
    return ResponseEntity.ok(new ApiResponse<>("success", "권한이 변경되었습니다", null));
}
```

## 1.4. 기능별 구현 (화면)

아래 항목은 기존 화면 기능 중 이번 회차에서 Spring Security 인증·인가와 MyBatis mapper 적용이 직접 드러나는 부분만 정리했습니다.

### 1.4.1. F903 · 로그인 사용자 관심지역 관리

`CustomSecurityConfig` 에서 `/favorite/**` 를 인증 필요 경로로 보호합니다. `FavoriteController` 는 `@AuthenticationPrincipal CustomUserDetails` 에서 현재 사용자 ID 를 가져오고, `FavoriteAreaMapper.xml` 은 해당 사용자 기준으로 관심지역을 조회·등록·삭제합니다.

**화면**

<img src="./images/screens/favorite_add.png" alt="관심지역 등록" style="max-width:900px;" />

_거래 검색 카드의 "관심지역" 버튼 → 우상단 토스트로 등록 완료 안내. 로그인하지 않은 사용자에게는 경고 메시지로 차단됩니다._

<img src="./images/screens/favorite_list.png" alt="관심지역 목록 + 삭제" style="max-width:520px;" />

_마이페이지의 관심지역 카드 — 저장된 시도·시군구·법정동 + LAWD_CD 를 표시하고, 각 행의 **삭제** 버튼으로 로그인 사용자 본인의 관심지역만 제거합니다._

### 1.4.2. F911 · 관리자 공지사항 관리

공지 목록과 상세 조회는 공개하고, `/notice/registerForm`, `/notice/editForm`, `/notice/register`, `/notice/edit`, `/notice/delete` 는 Spring Security 에서 `ROLE_ADMIN` 만 허용합니다. 공지 CRUD 와 상세 조회수 증가는 `NoticeMapper.xml` 로 처리합니다.

**화면**

<img src="./images/screens/notice_list.png" alt="공지사항 목록" style="max-width:900px;" />

_제목·내용 키워드 검색 + 최신순 정렬로 공지 목록을 표시. 관리자 로그인 시 우측 상단에 "+ 새 공지 등록" 버튼이 노출됩니다._

<img src="./images/screens/notice_detail.png" alt="공지사항 상세" style="max-width:900px;" />

_작성자·작성일·수정일·조회수 메타와 본문을 표시. 상세 조회 시 `view_count` 가 트랜잭션 안에서 1 증가합니다._

### 1.4.3. F912 · Spring Security 회원 인증과 회원 정보 관리

`CustomUserDetailsService` 는 `UserAccountMapper.xml` 의 `findByUserId` 결과를 `CustomUserDetails` 로 감싸 Spring Security 인증에 사용합니다. 회원 등록·정보 수정·탈퇴도 `UserAccountMapper.xml` 의 `insert`, `update`, `delete` 쿼리로 전환했습니다.

**화면**

<img src="./images/screens/register_form.png" alt="회원가입 폼" style="max-width:900px;" />

_아이디·이름·이메일·전화번호·비밀번호 입력 폼. 클라이언트에서 비밀번호 확인 일치 검증을 거친 뒤 `POST /member/register` 로 전송, 중복 아이디면 400 응답._

### 1.4.4. F912 · 마이페이지 회원 조회 / 정보 수정 / 비밀번호 변경

`/member/**` 와 `/mypage` 는 인증 사용자만 접근할 수 있습니다. 마이페이지의 회원 정보 수정과 비밀번호 변경은 인증 주체의 사용자 ID 로 처리하므로 요청 파라미터로 다른 회원을 수정하지 않습니다. 모든 회원 정보는 `UserAccountMapper.xml` 에서 조회·수정·삭제됩니다.

**화면**

<img src="./images/screens/mypage.png" alt="마이페이지 (본인 정보 / 관심지역 / 내가 쓴 글)" style="max-width:900px;" />

_아바타·아이디·이메일·전화번호·가입일/최근 수정 + 우측 관심지역 5건 + 하단 내가 쓴 글 (`GET /board/my` 로 본인 글만 조회) 을 한 화면에 모아 보여줍니다._

<img src="./images/screens/mypage_edit_info.png" alt="회원 정보 수정 모달" style="max-width:700px;" />

_"정보 수정" 버튼 → 이름·전화번호·이메일만 편집 (비밀번호 필드 없음). 저장 시 `POST /member/update` 호출 후 모달이 닫히면서 마이페이지 표시 값이 즉시 갱신됩니다._

<img src="./images/screens/mypage_change_password.png" alt="비밀번호 변경 모달" style="max-width:700px;" />

_"비밀번호 변경" 버튼 → 새 비밀번호·확인 입력. 두 값이 일치하지 않으면 모달 안에 빨간 alert 으로 즉시 경고._

### 1.4.5. F913 · 사용자 관리 (관리자)

`/admin/users`, `/member/list`, `/member/changeRole` 은 관리자 전용 경로입니다. `MemberController` 가 관리자 권한을 한 번 더 확인하고, 권한 변경은 `UserAccountMapper.xml` 의 `updateRole` 로 `users.role` 을 갱신합니다.

**화면**

<img src="./images/screens/admin_user_management.png" alt="사용자 관리 / 권한 토글" style="max-width:900px;" />

_전체 회원 목록 + 권한 뱃지(ADMIN/USER) + **"관리자로 승격"** 버튼. 본인 계정은 권한 변경 칸에 "본인" 라벨이 떠서 자기 자신을 강등할 수 없도록 차단됩니다._

### 1.4.6. F912 · 로그인 / 로그아웃

`POST /auth/login` 과 `POST /auth/logout` 은 `SecurityFilterChain` 의 form login 과 logout 설정에서 처리합니다. 로그인 성공 응답은 인증된 `CustomUserDetails` 의 사용자 정보를 JSON 으로 반환하고, 로그인 유지가 필요한 경우 Spring Security remember-me 설정을 사용합니다.

**화면**

<img src="./images/screens/login_form.png" alt="로그인 폼" style="max-width:900px;" />

_Spring Security 로그인 폼 — 입력한 `userId` 와 `password` 를 `SecurityFilterChain` 의 로그인 처리 URL 로 전송하고, 인증 이후 현재 사용자 정보는 `/auth/current` 로 확인합니다._

### 1.4.7. F920 · 관리자 주택 정보 수정

주택 목록과 상세는 공개 조회로 유지하고, 수정 폼과 수정 요청은 관리자만 접근하도록 보호했습니다. 수정 쿼리는 `HouseInfoMapper.xml` 의 `update` 에서 아파트명·건축년도·위도·경도를 갱신합니다.

**화면**

<img src="./images/screens/houseinfo_list.png" alt="주택 정보 목록" style="max-width:900px;" />

_시군구코드(LAWD_CD 5자리) + 법정동 + 아파트명 으로 검색. 결과는 거래수 많은 순으로 정렬되며, 관리자에게는 행마다 **수정** 버튼이 추가로 노출됩니다._

### 1.4.8. F921 · 공유게시판 권한 처리

게시글 목록과 상세는 공개하지만 글쓰기와 `/board/my` 는 로그인 사용자만 사용할 수 있습니다. 수정·삭제는 작성자 본인 또는 관리자만 가능하며, 게시글 조회·등록·수정·삭제와 작성자별 조회는 `BoardMapper.xml` 에서 처리합니다.

**화면**

<img src="./images/screens/board_list.png" alt="게시판 목록 + 검색" style="max-width:900px;" />

_제목·내용 키워드 검색 + 최신순 정렬. 로그인 사용자에게만 우측 상단 **"+ 글쓰기"** 버튼이 노출됩니다._

<img src="./images/screens/board_detail.png" alt="게시글 상세" style="max-width:900px;" />

_게시글 번호·작성자·작성일·수정일·조회수 메타와 본문 표시. 상세 진입 시 `view_count` 가 트랜잭션 안에서 1 증가._

<img src="./images/screens/board_form.png" alt="게시글 등록 폼" style="max-width:900px;" />

_제목 + 본문 (textarea) 입력 폼. 같은 폼이 등록 / 수정 양쪽에 재사용되며 `formAction` 값으로 `/board/register` vs `/board/edit` 가 결정됩니다._

<img src="./images/screens/board_edit_delete.png" alt="작성자/관리자 수정·삭제 버튼" style="max-width:900px;" />

_상세 페이지 우측 하단의 **수정 · 삭제** 버튼은 작성자 본인 또는 관리자에게만 노출. 비작성자에게는 빈 footer 만 보입니다._

---

**본 문서는 Spring Security 적용 과정과 구체적인 인증·인가 구현 사항을 중점으로 다루고 있습니다.**
