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
//  Step 3: 카테고리 내 증식 (비율 유지 버전)
// ============================================
    private final AtomicLong isbnCounter = new AtomicLong(1);
    @Test
    @DisplayName("Step 3: 시드 데이터 증식 → 비율 유지하며 정확히 2만건 추가")
    void step3_amplify() {
        // 1. 목표 설정
        int totalToAdd = 20000;
        long currentTotalCount = bookRepository.count();

        // ISBN 카운터 설정
        Long maxSuffix = bookRepository.findMaxStandardIdSuffix();
        isbnCounter.set(maxSuffix != null ? maxSuffix + 1 : 1);

        // 2. 모든 티어의 시드 데이터를 먼저 메모리에 로드
        int[][] allTierCids = {HEAVY_CIDS, MIDDLE_CIDS, MINOR_CIDS};
        List<Integer> allCids = Arrays.stream(allTierCids)
                .flatMapToInt(Arrays::stream)
                .boxed()
                .collect(Collectors.toList());

        // [중요] 분모 계산: 리스트에 로드된 모든 시드 데이터의 총 합계를 구함
        List<Book> allSeedsList = bookRepository.findByCategoryIdIn(allCids);
        long seedsTotalCount = allSeedsList.size();

        // 카테고리별로 그룹화
        Map<Category, List<Book>> seedsByCategory = allSeedsList.stream()
                .collect(Collectors.groupingBy(Book::getCategory));

        System.out.println("현재 시드 총합: " + seedsTotalCount + " | 목표 추가량: " + totalToAdd);

        String[] prefixes = {"개정판", "2024", "신판", "특별판", "리커버", "완전판", "문고판", "양장", "한정판", "축약판", "증보판", "2023", "2025", "미니북", "합본"};
        Map<String, Integer> tierDescConfig = Map.of("HEAVY", 3, "MIDDLE", 3, "MINOR", 2);

        int actualAddedTotal = 0;

        // 3. 티어 순회 (이미 로드된 데이터 활용)
        String[] tierNames = {"HEAVY", "MIDDLE", "MINOR"};
        for (int t = 0; t < allTierCids.length; t++) {
            String tier = tierNames[t];
            int descMergeCount = tierDescConfig.get(tier);

            // 해당 티어에 속한 CID 리스트
            List<Integer> currentTierCids = Arrays.stream(allTierCids[t]).boxed().collect(Collectors.toList());

            for (var entry : seedsByCategory.entrySet()) {
                // 현재 카테고리가 이번 티어에 속하는지 확인
                if (!currentTierCids.contains(entry.getKey().getId())) continue;

                Category category = entry.getKey();
                List<Book> seeds = entry.getValue();

                // [핵심] 이번 카테고리의 몫 계산
                // 공식: (이 카테고리 시드 수 / 리스트 전체 시드 수) * 20000
                int categoryQuota = (int) Math.round(((double) seeds.size() / seedsTotalCount) * totalToAdd);
                if (categoryQuota <= 0) categoryQuota = 1;

                List<Book> batch = new ArrayList<>();
                for (int i = 0; i < categoryQuota; i++) {
                    Book base = seeds.get(random.nextInt(seeds.size()));

                    // --- 1~6번 로직 (제목, 설명, 가격, 날짜, ISBN, 저자) ---
                    String newTitle = prefixes[random.nextInt(prefixes.length)] + " " + base.getTitle();
                    StringBuilder desc = new StringBuilder(base.getDescription() != null ? base.getDescription() : "");
                    for (int d = 1; d < descMergeCount; d++) {
                        Book other = seeds.get(random.nextInt(seeds.size()));
                        if (other.getDescription() != null && !other.getDescription().isBlank()) {
                            desc.append(" ").append(other.getDescription());
                        }
                    }
                    String finalDesc = desc.toString().isBlank() ? "도서 상세 정보가 준비 중입니다." : desc.toString();
                    int basePrice = (base.getPrice() != null && base.getPrice() > 0) ? base.getPrice() : 15000;
                    int variance = (int)(basePrice * 0.4 * random.nextDouble()) - (int)(basePrice * 0.2);
                    int newPrice = Math.max(1000, basePrice + variance);

                    LocalDate baseDate = base.getPublishedDate();
                    int maxDaysBack = tier.equals("HEAVY") ? 365 * 5 : (tier.equals("MIDDLE") ? 365 * 3 : 365 * 8);
                    LocalDate newDate = baseDate.minusDays(random.nextInt(maxDaysBack));
                    String newStandardId = "979" + String.format("%010d", isbnCounter.getAndIncrement());
                    String authorRaw = (base.getAuthor() != null ? base.getAuthor() : "미상") + " (지은이)";
                    if (base.getTranslator() != null && !base.getTranslator().isBlank()) authorRaw += ", " + base.getTranslator() + " (옮긴이)";

                    // --- 7) Book 생성 및 배치 저장 ---
                    Book amplified = new Book(category, newTitle, newStandardId, authorRaw, base.getPublisher(),
                            newDate.toString(), finalDesc, newPrice, base.getProductType().name(),
                            base.getThumbnailUrl(), base.getSeriesId(), base.getSeriesName());

                    batch.add(amplified);
                    if (batch.size() >= 1000) {
                        bookRepository.saveAll(batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    bookRepository.saveAll(batch);
                    batch.clear();
                }
                actualAddedTotal += categoryQuota;
            }
            System.out.println(tier + " 티어 처리 완료");
        }

        System.out.println("\n===== 최종 결과 =====");
        System.out.println("목표했던 추가 건수: " + totalToAdd);
        System.out.println("실제 추가된 건수: " + actualAddedTotal);
        System.out.println("현재 DB 총 건수: " + bookRepository.count());
    }
}
