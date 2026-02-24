package com.boogle.boogle.book.dummy;

import com.boogle.boogle.book.domain.Book;
import com.boogle.boogle.book.domain.Category;
import com.boogle.boogle.book.infra.BookRepository;
import com.boogle.boogle.book.infra.CategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@SpringBootTest
public class SeedDataCollectorTest {

    @Autowired
    private BookRepository bookRepository;
    @Autowired private CategoryRepository categoryRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${aladin.api.ttb-key}")
    private String TTB_KEY;
    private final Random random = new Random(42);
    // ============================================
    // 티어별 D2 카테고리 CID (알라딘 고유번호)
    // ============================================
    // Heavy 60%: 소설/시/희곡, 만화, 수험서/자격증
    static final int[] HEAVY_CIDS = {
            // 소설/시/희곡 D2 (15개)
            50917, 50918, 50919, 50920, 50921, 50922,
            50926, 50928, 50929, 50930, 50931, 50932,
            50933, 50935, 50940,
            // 만화 D2 (10개)
            3747, 4668, 3748, 3727, 3741,
            2561, 3750, 2552, 3728, 7443,
            // 수험서/자격증 D2 (6개)
            34582, 34690, 34921, 34700, 34705, 34895
    };
    // Middle 30%: 컴퓨터/모바일, 경제경영, 자기계발, 에세이, 인문학
    static final int[] MIDDLE_CIDS = {
            // 컴퓨터/모바일 D2 (7개)
            2630, 55977, 437, 7396, 2719, 363, 6348,
            // 경제경영 D2 (6개)
            3057, 2172, 2028, 261, 172, 177,
            // 자기계발 D2 (5개)
            70214, 2951, 70218, 70211, 70224,
            // 에세이 D2 (6개)
            51371, 51373, 51375, 51377, 51380, 51416,
            // 인문학 D2 (5개)
            51378, 51390, 51393, 51395, 51387
    };
    // Minor 10%: 종교, 잡지, 과학, 여행, 예술
    static final int[] MINOR_CIDS = {
            // 종교/역학 D2 (3개)
            51565, 51564, 51566,
            // 잡지 D2 (3개)
            7607, 7605, 2925,
            // 과학 D2 (3개)
            51002, 51024, 51033,
            // 여행 D2 (3개)
            50830, 50831, 50832,
            // 예술/대중문화 D2 (3개)
            50965, 50966, 50967
    };
    // ============================================
    //  Step 1: 시드 수집
    // ============================================
    @Test
    @DisplayName("Step 1: 알라딘 ItemList API → DB 시드 수집")
    void step1_collectSeeds() throws InterruptedException {
        int[][] allTiers = {HEAVY_CIDS, MIDDLE_CIDS, MINOR_CIDS};
        String[] tierNames = {"HEAVY", "MIDDLE", "MINOR"};
        int totalSaved = 0;
        for (int t = 0; t < allTiers.length; t++) {
            int[] cids = allTiers[t];
            int tierSaved = 0;
            System.out.println("\n===== " + tierNames[t] + " 티어 수집 시작 ("
                    + cids.length + "개 카테고리) =====");
            for (int categoryId : cids) {
                int catSaved = 0;
                for (int page = 1; page <= 4; page++) {
                    String url = String.format(
                            "http://www.aladin.co.kr/ttb/api/ItemList.aspx?"
                                    + "ttbkey=%s&QueryType=ItemNewAll&MaxResults=50&start=%d"
                                    + "&SearchTarget=Book&output=js&Version=20131101"
                                    + "&CategoryId=%d&Cover=Big",
                            TTB_KEY, page, categoryId
                    );
                    try {
                        String json = restTemplate.getForObject(url, String.class);
                        List<Book> books = parseResponse(json);
                        int saved = 0;
                        for (Book book : books) {
                            if (!bookRepository.existsByStandardId(
                                    book.getStandardId())) {
                                bookRepository.save(book);
                                saved++;
                            }
                        }
                        catSaved += saved;
                    } catch (Exception e) {
                        System.err.println("⚠ API 에러 [CID:" + categoryId
                                + " P:" + page + "] " + e.getMessage());
                    }
                }
                Thread.sleep(300); // Rate Limit 방어
                tierSaved += catSaved;
                System.out.println("  [CID:" + categoryId + "] "
                        + catSaved + "건 저장");
            }
            totalSaved += tierSaved;
            System.out.println("===== " + tierNames[t] + " 티어 완료: "
                    + tierSaved + "건 =====");
        }
        System.out.println("\n Step 1 완료! 총 시드: " + totalSaved + "건");
        System.out.println("DB 전체 건수: " + bookRepository.count());
    }
    // ============================================
    // 응답 파싱 (공통)
    // ============================================
    private List<Book> parseResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode items = root.get("item");
        List<Book> books = new ArrayList<>();
        if (items == null || !items.isArray()) return books;
        for (JsonNode item : items) {
            try {
                // --- 카테고리 처리 ---
                int catId = item.path("categoryId").asInt(0);
                String catName = item.path("categoryName").asText("");
                if (catId == 0) continue;
                Category category = categoryRepository.findById(catId)
                        .orElseGet(() -> {
                            Category newCat = Category.createCategory(catId, catName);
                            return categoryRepository.save(newCat);
                        });
                // --- 필수 필드 검증 ---
                        String isbn13 = item.path("isbn13").asText("");
                String title = item.path("title").asText("");
                String author = item.path("author").asText("");
                String publisher = item.path("publisher").asText("");
                String pubDate = item.path("pubDate").asText("");
                if (isbn13.isBlank() || title.isBlank()
                        || publisher.isBlank() || pubDate.isBlank()) {
                    continue; // 필수값 누락 → skip
                }
                // author가 빈 경우 기본값
                if (author.isBlank()) author = "미상 (지은이)";
                // --- 선택 필드 ---
                        String description = item.path("description").asText("");
                int price = item.path("priceStandard").asInt(0);
                String cover = item.path("cover").asText("");
                String mallType = item.path("mallType").asText("Book");
                String productType = mallType.equalsIgnoreCase("FOREIGN")
                        ? "FOREIGN" : "DOMESTIC";
                // --- 시리즈 정보 ---
                        Integer seriesId = null;
                String seriesName = null;
                JsonNode seriesInfo = item.get("seriesInfo");
                if (seriesInfo != null && !seriesInfo.isNull()) {
                    int sid = seriesInfo.path("seriesId").asInt(0);
                    seriesId = (sid != 0) ? sid : null;
                    seriesName = seriesInfo.path("seriesName").asText(null);
                }
                // --- Book 생성 ---
                //생성자 내부에서 author/translator 자동 분리됨
                Book book = new Book(
                        category, title, isbn13, author, publisher,
                        pubDate, description, price, productType,
                        cover, seriesId, seriesName
                );
                books.add(book);
            } catch (Exception e) {
                // 개별 아이템 파싱 실패 → skip
            }
        }
        return books;
    }



    // ============================================
    //  Step 2: Heavy 시드 description 보강
    // ============================================
    @Test
    @DisplayName("Step 2: Heavy 시드 → 같은 카테고리 description 교차 합성")
    void step2_enrichDescription() {
        List<Integer> heavyCids = Arrays.stream(HEAVY_CIDS)
                .boxed().collect(Collectors.toList());

        // 카테고리별로 그룹핑
        Map<Integer, List<Book>> seedsByCategory =
                bookRepository.findByCategoryIdIn(heavyCids).stream()
                        .collect(Collectors.groupingBy(b -> b.getCategory().getId()));

        Random random = new Random(42);
        int enriched = 0;

        for (var entry : seedsByCategory.entrySet()) {
            List<Book> seeds = entry.getValue();
            if (seeds.size() < 3) continue; // 시드가 너무 적으면 skip

            for (Book book : seeds) {
                StringBuilder sb = new StringBuilder(
                        book.getDescription() != null ? book.getDescription() : ""
                );

                // 같은 카테고리의 다른 책 description을 합쳐서 1,000자 이상으로
                while (sb.length() < 1000) {
                    Book other = seeds.get(random.nextInt(seeds.size()));
                    String otherDesc = other.getDescription();
                    if (otherDesc != null && !otherDesc.isBlank()) {
                        sb.append(" ").append(otherDesc);
                    } else {
                        // description이 없는 책이면 제목+저자라도 합침
                        sb.append(" ").append(other.getTitle())
                                .append(" ").append(other.getAuthor());
                    }
                }

                book.enrichDescription(sb.toString());
                bookRepository.save(book);
                enriched++;
            }
        }

        System.out.println("Step 2 완료! 보강: " + enriched + "건");
    }

    // ============================================
    //  Step 3: 카테고리 내 증식
    // ============================================
    private final AtomicLong isbnCounter = new AtomicLong(1);
    @Test
    @DisplayName("Step 3: 시드 데이터 증식 → 9만건")
    void step3_amplify() {
        String[] prefixes = {
                "개정판", "2024", "신판", "특별판", "리커버",
                "완전판", "문고판", "양장", "한정판", "축약판",
                "증보판", "2023", "2025", "미니북", "합본"
        };
        // 티어별 설정: [증식 배수, description 합침 횟수]
        Map<String, int[]> tierConfig = Map.of(
                "HEAVY",  new int[]{16, 3},   // 247 × 3 = ~741자
                "MIDDLE", new int[]{7, 3},    // 150 × 3 = ~450자
                "MINOR",  new int[]{7, 2}     // 150 × 2 = ~300자
        );
        int[][] allTiers = {HEAVY_CIDS, MIDDLE_CIDS, MINOR_CIDS};
        String[] tierNames = {"HEAVY", "MIDDLE", "MINOR"};
        int grandTotal = 0;
        for (int t = 0; t < allTiers.length; t++) {
            String tier = tierNames[t];
            int multiplier = tierConfig.get(tier)[0];
            int descMergeCount = tierConfig.get(tier)[1];
            List<Integer> cids = Arrays.stream(allTiers[t])
                    .boxed().collect(Collectors.toList());
            Map<Category, List<Book>> seedsByCategory =
                    bookRepository.findByCategoryIdIn(cids).stream()
                            .collect(Collectors.groupingBy(Book::getCategory));
            int tierTotal = 0;
            System.out.println("\n===== " + tier + " 증식 시작 (×"
                    + multiplier + ") =====");
            for (var entry : seedsByCategory.entrySet()) {
                Category category = entry.getKey();
                List<Book> seeds = entry.getValue();
                if (seeds.isEmpty()) continue;
                int targetCount = seeds.size() * multiplier;
                List<Book> batch = new ArrayList<>();
                for (int i = 0; i < targetCount; i++) {
                    Book base = seeds.get(random.nextInt(seeds.size()));
                    // --- 1) 제목 변형 ---
                            String newTitle = prefixes[random.nextInt(prefixes.length)]
                            + " " + base.getTitle();
                    // --- 2) Description 합치기 ---
                    StringBuilder desc = new StringBuilder(
                            base.getDescription() != null
                                    ? base.getDescription() : ""
                    );
                    for (int d = 1; d < descMergeCount; d++) {
                        Book other = seeds.get(random.nextInt(seeds.size()));
                        if (other.getDescription() != null
                                && !other.getDescription().isBlank()) {
                            desc.append(" ").append(other.getDescription());
                        }
                    }
                    String finalDesc = desc.toString().isBlank()
                            ? "도서 상세 정보가 준비 중입니다."
                        : desc.toString();
                    // --- 3) 가격 ±20% 변동 ---
                    int basePrice = (base.getPrice() != null && base.getPrice() > 0)
                            ? base.getPrice() : 15000;
                    int variance = (int)(basePrice * 0.4 * random.nextDouble())
                            - (int)(basePrice * 0.2);
                    int newPrice = Math.max(1000, basePrice + variance);
                    // --- 4) 출간일 랜덤 분산 ---
                            LocalDate baseDate = base.getPublishedDate();
                    int maxDaysBack = switch (tier) {
                        case "HEAVY"  -> 365 * 5;   // 최근 5년
                        case "MIDDLE" -> 365 * 3;   // 최근 3년
                        default       -> 365 * 8;   // 최근 8년
                    };
                    LocalDate newDate = baseDate.minusDays(
                            random.nextInt(maxDaysBack));
                    // 2000년 이전 방지 & 미래 날짜 방지
                    if (newDate.isBefore(LocalDate.of(2000, 1, 1))) {
                        newDate = LocalDate.of(2000, 1, 1)
                                .plusDays(random.nextInt(365 * 10));
                    }
                    if (newDate.isAfter(LocalDate.now())) {
                        newDate = LocalDate.now()
                                .minusDays(random.nextInt(365));
                    }
                    // --- 5) 유니크 ISBN (순차 카운터) ---
                            String newStandardId = "979"
                            + String.format("%010d", isbnCounter.getAndIncrement());
                    // --- 6) author 재조합 ---
                    String authorRaw = base.getAuthor() != null
                            ? base.getAuthor() + " (지은이)" : "미상 (지은이)";
                    if (base.getTranslator() != null
                            && !base.getTranslator().isBlank()) {
                        authorRaw += ", " + base.getTranslator() + " (옮긴이)";
                    }
                    // --- 7) Book 생성 ---
                    Book amplified = new Book(
                            category,
                            newTitle,
                            newStandardId,
                            authorRaw,
                            base.getPublisher(),
                            newDate.toString(),
                            finalDesc,
                            newPrice,
                            base.getProductType().name(),
                            base.getThumbnailUrl(),
                            base.getSeriesId(),
                            base.getSeriesName()
                    );
                    batch.add(amplified);
                    // 1,000건 단위 배치 저장
                    if (batch.size() >= 1000) {
                        bookRepository.saveAll(batch);
                        batch.clear();
                    }
                }
                // 남은 건 저장
                if (!batch.isEmpty()) {
                    bookRepository.saveAll(batch);
                    batch.clear();
                }
                tierTotal += targetCount;
            }
            grandTotal += tierTotal;
            System.out.println("===== " + tier + " 증식 완료: "
                    + tierTotal + "건 =====");
        }
        long dbCount = bookRepository.count();
        System.out.println("\n Step 3 완료!");
        System.out.println("증식 건수: " + grandTotal);
        System.out.println("DB 전체 건수: " + dbCount);
    }

}