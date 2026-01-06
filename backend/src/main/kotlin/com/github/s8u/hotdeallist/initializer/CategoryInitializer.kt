package com.github.s8u.hotdeallist.initializer

import com.github.s8u.hotdeallist.entity.Category
import com.github.s8u.hotdeallist.repository.CategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class CategoryInitializer(
    private val categoryRepository: CategoryRepository
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        logger.info("카테고리 초기화를 시작합니다...")
        initializeCategories()
        logger.info("카테고리 초기화가 완료되었습니다. 총 {}개의 카테고리가 생성/업데이트되었습니다.", categoryRepository.count())
    }

    private fun initializeCategories() {
        var sortOrder = 0

        // 1. 전자·가전
        val electronics = upsertCategory("electronics", "전자·가전", "Electronics", depth = 0, sortOrder = sortOrder++)
        
        // 1-1. 모바일
        val mobile = upsertCategory("mobile", "모바일", "Mobile", electronics.id!!, 1, sortOrder++)
        upsertCategory("smartphone", "스마트폰", "Smartphone", mobile.id!!, 2, sortOrder++)
        upsertCategory("tablet", "태블릿", "Tablet", mobile.id!!, 2, sortOrder++)
        upsertCategory("smartwatch", "스마트워치 / 밴드", "Smartwatch / Band", mobile.id!!, 2, sortOrder++)
        // 1-1-1. 액세서리
        val mobileAccessory = upsertCategory("mobile_accessory", "액세서리", "Accessory", mobile.id!!, 2, sortOrder++)
        upsertCategory("case", "케이스", "Case", mobileAccessory.id!!, 3, sortOrder++)
        upsertCategory("screen_protector", "보호필름", "Screen Protector", mobileAccessory.id!!, 3, sortOrder++)
        upsertCategory("charger_cable", "충전기 / 케이블", "Charger / Cable", mobileAccessory.id!!, 3, sortOrder++)

        // 1-2. 컴퓨터
        val computer = upsertCategory("computer", "컴퓨터", "Computer", electronics.id!!, 1, sortOrder++)
        upsertCategory("laptop", "노트북", "Laptop", computer.id!!, 2, sortOrder++)
        upsertCategory("desktop", "데스크톱", "Desktop", computer.id!!, 2, sortOrder++)
        // 1-2-1. 컴퓨터 부품
        val computerParts = upsertCategory("computer_parts", "컴퓨터 부품", "Computer Parts", computer.id!!, 2, sortOrder++)
        upsertCategory("cpu", "CPU", "CPU", computerParts.id!!, 3, sortOrder++)
        upsertCategory("mainboard", "메인보드", "Mainboard", computerParts.id!!, 3, sortOrder++)
        upsertCategory("ram", "RAM", "RAM", computerParts.id!!, 3, sortOrder++)
        upsertCategory("gpu", "그래픽카드", "Graphics Card", computerParts.id!!, 3, sortOrder++)
        // 1-2-1-1. 저장장치
        val storage = upsertCategory("storage", "저장장치", "Storage", computerParts.id!!, 3, sortOrder++)
        upsertCategory("ssd", "SSD", "SSD", storage.id!!, 4, sortOrder++)
        upsertCategory("hdd", "HDD", "HDD", storage.id!!, 4, sortOrder++)
        upsertCategory("nas", "NAS", "NAS", storage.id!!, 4, sortOrder++)
        upsertCategory("external_storage", "외장 저장장치", "External Storage", storage.id!!, 4, sortOrder++)
        upsertCategory("psu", "파워서플라이", "Power Supply", computerParts.id!!, 3, sortOrder++)
        upsertCategory("computer_case", "케이스", "Case", computerParts.id!!, 3, sortOrder++)
        // 1-2-1-2. 쿨링
        val cooling = upsertCategory("cooling", "쿨링", "Cooling", computerParts.id!!, 3, sortOrder++)
        upsertCategory("cpu_cooler", "CPU 쿨러", "CPU Cooler", cooling.id!!, 4, sortOrder++)
        upsertCategory("case_fan", "케이스 팬", "Case Fan", cooling.id!!, 4, sortOrder++)
        upsertCategory("water_cooling", "수랭 쿨러", "Water Cooling", cooling.id!!, 4, sortOrder++)
        // 1-2-1-3. 확장카드
        val expansionCard = upsertCategory("expansion_card", "확장카드", "Expansion Card", computerParts.id!!, 3, sortOrder++)
        upsertCategory("sound_card", "사운드카드", "Sound Card", expansionCard.id!!, 4, sortOrder++)
        upsertCategory("network_card", "네트워크카드", "Network Card", expansionCard.id!!, 4, sortOrder++)
        upsertCategory("cable_gender", "케이블 / 젠더", "Cable / Gender", computerParts.id!!, 3, sortOrder++)
        // 1-2-2. 주변기기
        val peripheral = upsertCategory("peripheral", "주변기기", "Peripheral", computer.id!!, 2, sortOrder++)
        upsertCategory("monitor", "모니터", "Monitor", peripheral.id!!, 3, sortOrder++)
        upsertCategory("keyboard", "키보드", "Keyboard", peripheral.id!!, 3, sortOrder++)
        upsertCategory("mouse", "마우스", "Mouse", peripheral.id!!, 3, sortOrder++)
        upsertCategory("printer_scanner", "프린터 / 스캐너", "Printer / Scanner", peripheral.id!!, 3, sortOrder++)
        upsertCategory("webcam", "웹캠", "Webcam", peripheral.id!!, 3, sortOrder++)
        upsertCategory("usb", "USB", "USB", peripheral.id!!, 3, sortOrder++)

        // 1-3. 영상·음향
        val av = upsertCategory("av", "영상·음향", "Audio / Video", electronics.id!!, 1, sortOrder++)
        upsertCategory("tv", "TV", "TV", av.id!!, 2, sortOrder++)
        upsertCategory("mic", "마이크", "Microphone", av.id!!, 2, sortOrder++)
        upsertCategory("speaker", "스피커", "Speaker", av.id!!, 2, sortOrder++)
        upsertCategory("earphone_headphone", "이어폰 / 헤드폰", "Earphone / Headphone", av.id!!, 2, sortOrder++)
        upsertCategory("home_theater", "홈시어터", "Home Theater", av.id!!, 2, sortOrder++)

        // 1-4. 게임
        val game = upsertCategory("game", "게임", "Game", electronics.id!!, 1, sortOrder++)
        upsertCategory("console", "콘솔", "Console", game.id!!, 2, sortOrder++)
        upsertCategory("vr", "VR 기기", "VR Device", game.id!!, 2, sortOrder++)
        upsertCategory("game_software", "게임 소프트웨어", "Game Software", game.id!!, 2, sortOrder++)
        upsertCategory("game_peripheral", "주변기기", "Peripheral", game.id!!, 2, sortOrder++)

        // 1-5. 생활가전
        val appliance = upsertCategory("appliance", "생활가전", "Home Appliance", electronics.id!!, 1, sortOrder++)
        upsertCategory("refrigerator", "냉장고", "Refrigerator", appliance.id!!, 2, sortOrder++)
        upsertCategory("washing_machine", "세탁기", "Washing Machine", appliance.id!!, 2, sortOrder++)
        upsertCategory("vacuum", "청소기", "Vacuum Cleaner", appliance.id!!, 2, sortOrder++)
        upsertCategory("air_purifier", "공기청정기", "Air Purifier", appliance.id!!, 2, sortOrder++)
        upsertCategory("kitchen_appliance", "주방가전", "Kitchen Appliance", appliance.id!!, 2, sortOrder++)

        // 2. 자동차·공구
        val autoTools = upsertCategory("auto_tools", "자동차·공구", "Auto & Tools", depth = 0, sortOrder = sortOrder++)
        
        // 2-1. 자동차용품
        val autoAccessory = upsertCategory("auto_accessory", "자동차용품", "Auto Accessory", autoTools.id!!, 1, sortOrder++)
        upsertCategory("blackbox", "블랙박스", "Blackbox", autoAccessory.id!!, 2, sortOrder++)
        upsertCategory("car_accessory", "차량용 액세서리", "Car Accessory", autoAccessory.id!!, 2, sortOrder++)
        upsertCategory("tire_wheel", "타이어·휠", "Tire & Wheel", autoAccessory.id!!, 2, sortOrder++)
        
        // 2-2. 공구
        val tools = upsertCategory("tools", "공구", "Tools", autoTools.id!!, 1, sortOrder++)
        upsertCategory("power_tool", "전동공구", "Power Tool", tools.id!!, 2, sortOrder++)
        upsertCategory("hand_tool", "수공구", "Hand Tool", tools.id!!, 2, sortOrder++)

        // 3. 패션·의류
        val fashion = upsertCategory("fashion", "패션·의류", "Fashion", depth = 0, sortOrder = sortOrder++)
        
        // 3-1. 남성의류
        val menClothing = upsertCategory("men_clothing", "남성의류", "Men's Clothing", fashion.id!!, 1, sortOrder++)
        upsertCategory("men_top", "상의", "Top", menClothing.id!!, 2, sortOrder++)
        upsertCategory("men_bottom", "하의", "Bottom", menClothing.id!!, 2, sortOrder++)
        upsertCategory("men_outer", "아우터", "Outer", menClothing.id!!, 2, sortOrder++)
        upsertCategory("suit", "정장", "Suit", menClothing.id!!, 2, sortOrder++)
        
        // 3-2. 여성의류
        val womenClothing = upsertCategory("women_clothing", "여성의류", "Women's Clothing", fashion.id!!, 1, sortOrder++)
        upsertCategory("women_top", "상의", "Top", womenClothing.id!!, 2, sortOrder++)
        upsertCategory("women_bottom", "하의", "Bottom", womenClothing.id!!, 2, sortOrder++)
        upsertCategory("dress", "원피스", "Dress", womenClothing.id!!, 2, sortOrder++)
        upsertCategory("women_outer", "아우터", "Outer", womenClothing.id!!, 2, sortOrder++)
        
        // 3-3. 패션잡화
        val fashionAccessory = upsertCategory("fashion_accessory", "패션잡화", "Fashion Accessory", fashion.id!!, 1, sortOrder++)
        upsertCategory("bag", "가방", "Bag", fashionAccessory.id!!, 2, sortOrder++)
        upsertCategory("shoes", "신발", "Shoes", fashionAccessory.id!!, 2, sortOrder++)
        upsertCategory("wallet", "지갑", "Wallet", fashionAccessory.id!!, 2, sortOrder++)
        upsertCategory("hat_belt", "모자 / 벨트", "Hat / Belt", fashionAccessory.id!!, 2, sortOrder++)

        // 4. 뷰티·미용
        val beauty = upsertCategory("beauty", "뷰티·미용", "Beauty", depth = 0, sortOrder = sortOrder++)
        upsertCategory("skincare", "스킨케어", "Skincare", beauty.id!!, 1, sortOrder++)
        upsertCategory("makeup", "메이크업", "Makeup", beauty.id!!, 1, sortOrder++)
        upsertCategory("hair_body", "헤어·바디", "Hair & Body", beauty.id!!, 1, sortOrder++)

        // 5. 식품
        val food = upsertCategory("food", "식품", "Food", depth = 0, sortOrder = sortOrder++)
        
        // 5-1. 신선식품
        val freshFood = upsertCategory("fresh_food", "신선식품", "Fresh Food", food.id!!, 1, sortOrder++)
        upsertCategory("fruit", "과일", "Fruit", freshFood.id!!, 2, sortOrder++)
        upsertCategory("vegetable", "채소", "Vegetable", freshFood.id!!, 2, sortOrder++)
        upsertCategory("meat_seafood", "육류 / 해산물", "Meat / Seafood", freshFood.id!!, 2, sortOrder++)
        
        // 5-2. 가공식품
        val processedFood = upsertCategory("processed_food", "가공식품", "Processed Food", food.id!!, 1, sortOrder++)
        upsertCategory("snack", "스낵 / 과자", "Snack", processedFood.id!!, 2, sortOrder++)
        upsertCategory("chocolate_candy", "초콜릿 / 사탕", "Chocolate / Candy", processedFood.id!!, 2, sortOrder++)
        upsertCategory("bakery", "베이커리", "Bakery", processedFood.id!!, 2, sortOrder++)
        upsertCategory("frozen_food", "냉동식품", "Frozen Food", processedFood.id!!, 2, sortOrder++)
        upsertCategory("instant_food", "즉석식품", "Instant Food", processedFood.id!!, 2, sortOrder++)
        upsertCategory("canned_food", "통조림", "Canned Food", processedFood.id!!, 2, sortOrder++)
        
        // 5-3. 간편식
        val readyMeal = upsertCategory("ready_meal", "간편식", "Ready Meal", food.id!!, 1, sortOrder++)
        upsertCategory("meal_kit", "밀키트", "Meal Kit", readyMeal.id!!, 2, sortOrder++)
        upsertCategory("lunchbox", "도시락", "Lunchbox", readyMeal.id!!, 2, sortOrder++)
        upsertCategory("ramen", "라면", "Ramen", readyMeal.id!!, 2, sortOrder++)
        
        // 5-4. 음료
        val beverage = upsertCategory("beverage", "음료", "Beverage", food.id!!, 1, sortOrder++)
        upsertCategory("coffee", "커피", "Coffee", beverage.id!!, 2, sortOrder++)
        upsertCategory("tea", "차", "Tea", beverage.id!!, 2, sortOrder++)
        upsertCategory("juice_soda", "주스 / 탄산", "Juice / Soda", beverage.id!!, 2, sortOrder++)

        // 6. 생활·가구
        val living = upsertCategory("living", "생활·가구", "Living & Furniture", depth = 0, sortOrder = sortOrder++)
        
        // 6-1. 가구
        val furniture = upsertCategory("furniture", "가구", "Furniture", living.id!!, 1, sortOrder++)
        upsertCategory("bed", "침대", "Bed", furniture.id!!, 2, sortOrder++)
        upsertCategory("desk", "책상", "Desk", furniture.id!!, 2, sortOrder++)
        upsertCategory("chair", "의자", "Chair", furniture.id!!, 2, sortOrder++)
        upsertCategory("storage_furniture", "수납장", "Storage", furniture.id!!, 2, sortOrder++)
        
        // 6-2. 인테리어
        val interior = upsertCategory("interior", "인테리어", "Interior", living.id!!, 1, sortOrder++)
        upsertCategory("lighting", "조명", "Lighting", interior.id!!, 2, sortOrder++)
        upsertCategory("curtain", "커튼", "Curtain", interior.id!!, 2, sortOrder++)
        upsertCategory("interior_accessory", "소품", "Accessory", interior.id!!, 2, sortOrder++)
        
        // 6-3. 생활용품
        val livingGoods = upsertCategory("living_goods", "생활용품", "Living Goods", living.id!!, 1, sortOrder++)
        upsertCategory("kitchen_goods", "주방용품", "Kitchen Goods", livingGoods.id!!, 2, sortOrder++)
        upsertCategory("bathroom_goods", "욕실용품", "Bathroom Goods", livingGoods.id!!, 2, sortOrder++)
        upsertCategory("cleaning_goods", "청소용품", "Cleaning Goods", livingGoods.id!!, 2, sortOrder++)

        // 7. 취미·레저
        val hobby = upsertCategory("hobby", "취미·레저", "Hobby & Leisure", depth = 0, sortOrder = sortOrder++)
        
        // 7-1. 스포츠
        val sports = upsertCategory("sports", "스포츠", "Sports", hobby.id!!, 1, sortOrder++)
        upsertCategory("fitness", "헬스", "Fitness", sports.id!!, 2, sortOrder++)
        upsertCategory("bicycle", "자전거", "Bicycle", sports.id!!, 2, sortOrder++)
        upsertCategory("camping", "캠핑", "Camping", sports.id!!, 2, sortOrder++)
        
        // 7-2. 도서·문구
        val bookStationery = upsertCategory("book_stationery", "도서·문구", "Book & Stationery", hobby.id!!, 1, sortOrder++)
        upsertCategory("book", "도서", "Book", bookStationery.id!!, 2, sortOrder++)
        upsertCategory("stationery", "문구", "Stationery", bookStationery.id!!, 2, sortOrder++)
        upsertCategory("diary", "다이어리", "Diary", bookStationery.id!!, 2, sortOrder++)

        // 8. 기타
        val etc = upsertCategory("etc", "기타", "Etc", depth = 0, sortOrder = sortOrder++)
        upsertCategory("subscription", "구독·이용권", "Subscription", etc.id!!, 1, sortOrder++)
        upsertCategory("gift_card", "상품권", "Gift Card", etc.id!!, 1, sortOrder++)
        upsertCategory("gifticon", "기프티콘", "Gifticon", etc.id!!, 1, sortOrder++)
        upsertCategory("point", "포인트", "Point", etc.id!!, 1, sortOrder++)
    }

    private fun upsertCategory(
        code: String,
        name: String,
        nameEn: String,
        parentId: Long? = null,
        depth: Int,
        sortOrder: Int
    ): Category {
        val existingCategory = categoryRepository.findByCode(code)
        
        return if (existingCategory != null) {
            // 기존 카테고리 업데이트
            existingCategory.apply {
                this.name = name
                this.nameEn = nameEn
                this.parentId = parentId
                this.depth = depth
                this.sortOrder = sortOrder
            }
            categoryRepository.save(existingCategory)
        } else {
            // 새 카테고리 생성
            val category = Category(
                parentId = parentId,
                code = code,
                name = name,
                nameEn = nameEn,
                depth = depth,
                sortOrder = sortOrder
            )
            categoryRepository.save(category)
        }
    }
}
