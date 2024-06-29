package com.money.monocle.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.money.monocle.R
import java.util.UUID

/**
 * @param category A common **enum** category
 * @param categoryId A unique category id created by a user
 */
data class Record(
    val expense: Boolean = false,
    val id: String = "",
    val category: String = "",
    val categoryId: String? = null,
    val date: Long = 0,
    val timestamp: Long = 0,
    val amount: Float = 0f
)

/**
 * @param category A common enum category
 * @param name A name given to that category either by default or by a user
 */
data class Category(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "",
    val name: String = "",
    val res: Int? = null,
    val timestamp: Long? = null
)

data class FirestoreCategory(
    val id: String = "",
    val category: String = "",
    val name: String = "",
    val timestamp: Long? = null
)

data class RawCategory(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String = "",
    @DrawableRes val res: Int? = null,
    @StringRes val name: Int? = null
)



val defaultRawExpenseCategories = listOf(
    RawCategory(categoryId = DefaultExpenseCategoriesIds.ENTERTAINMENT.name,
        res = R.drawable.entertainment, name = R.string.entertainment
    ),
    RawCategory(categoryId = DefaultExpenseCategoriesIds.GROCERIES.name, res = R.drawable.groceries, name = R.string.groceries),
    RawCategory(categoryId = DefaultExpenseCategoriesIds.INSURANCE.name, res = R.drawable.insurance, name = R.string.insurance),
    RawCategory(categoryId = DefaultExpenseCategoriesIds.TRANSPORTATION.name, res = R.drawable.transportation,name =  R.string.transportation),
    RawCategory(categoryId = DefaultExpenseCategoriesIds.UTILITIES.name, res = R.drawable.utilities,name =  R.string.utilities),
)
val defaultRawIncomeCategories = listOf(
    RawCategory(categoryId = DefaultIncomeCategoriesIds.WAGE.name, res = R.drawable.wage,name =  R.string.wage),
    RawCategory(categoryId = DefaultIncomeCategoriesIds.BUSINESS.name, res = R.drawable.business,name =  R.string.business),
    RawCategory(categoryId = DefaultIncomeCategoriesIds.INTEREST.name, res = R.drawable.interest,name =  R.string.interest),
    RawCategory(categoryId = DefaultIncomeCategoriesIds.INVESTMENT.name, res = R.drawable.investment,name =  R.string.investment),
    RawCategory(categoryId = DefaultIncomeCategoriesIds.GIFT.name, res = R.drawable.gift,name =  R.string.gift),
    RawCategory(categoryId = DefaultIncomeCategoriesIds.GOVERNMENT_PAYMENT.name, res = R.drawable.government_payment,name =  R.string.government_payment),
)



enum class DefaultExpenseCategoriesIds {
    ENTERTAINMENT,
    GROCERIES,
    INSURANCE,
    TRANSPORTATION,
    UTILITIES
}
enum class DefaultIncomeCategoriesIds {
    WAGE,
    BUSINESS,
    INTEREST,
    INVESTMENT,
    GIFT,
    GOVERNMENT_PAYMENT
}

object CustomRawExpenseCategories {
    val categories = mapOf(
        R.string.entertainment to listOf(
            RawCategory(categoryId = CustomExpenseCategoriesIds.MOVIE.name, res = R.drawable.movie),
            RawCategory(categoryId = CustomExpenseCategoriesIds.STREAMING_SERVICE.name, res = R.drawable.streaming_service),
            RawCategory(categoryId = CustomExpenseCategoriesIds.VIDEO_GAME.name, res = R.drawable.video_game),
        ),
        R.string.food to listOf(
            RawCategory(categoryId = CustomExpenseCategoriesIds.FOOD_DELIVERY.name, res = R.drawable.food_delivery),
            RawCategory(categoryId = CustomExpenseCategoriesIds.RESTAURANT.name, res = R.drawable.restaurant),
        ),
        R.string.health to listOf(
            RawCategory(categoryId = CustomExpenseCategoriesIds.DOCTOR.name, res = R.drawable.doctor),
            RawCategory(categoryId = CustomExpenseCategoriesIds.HEALTH_INSURANCE.name, res = R.drawable.health_insurance),
            RawCategory(categoryId = CustomExpenseCategoriesIds.MENTAL_HEALTH.name, res = R.drawable.mental_health),
            RawCategory(categoryId = CustomExpenseCategoriesIds.PRESCRIPTION.name, res = R.drawable.prescription)
        ),
        R.string.housing to listOf(
            RawCategory(categoryId = CustomExpenseCategoriesIds.HOUSE_MAINTENANCE.name, res = R.drawable.house_maintenance),
            RawCategory(categoryId = CustomExpenseCategoriesIds.PROPERTY_TAX.name, res = R.drawable.property_tax),
            RawCategory(categoryId = CustomExpenseCategoriesIds.RENT.name, res = R.drawable.rent),
            RawCategory(categoryId = CustomExpenseCategoriesIds.RENTERS_INSURANCE.name, res = R.drawable.renters_insurance)
        ),
        R.string.personal_care to listOf(
            RawCategory(categoryId = CustomExpenseCategoriesIds.CLOTHING.name, res = R.drawable.clothing),
            RawCategory(categoryId = CustomExpenseCategoriesIds.COSMETICS_SKINCARE.name, res = R.drawable.cosmetics_skincare),
            RawCategory(categoryId = CustomExpenseCategoriesIds.FITNESS.name, res = R.drawable.fitness),
            RawCategory(categoryId = CustomExpenseCategoriesIds.HAIRCUT_STYLING.name, res = R.drawable.haircut_styling)
        ),
        R.string.transportation to listOf(
            RawCategory(categoryId = CustomExpenseCategoriesIds.CAR_INSURANCE.name, res = R.drawable.car_insurance),
            RawCategory(categoryId = CustomExpenseCategoriesIds.CAR_REPAIR.name, res = R.drawable.car_repair),
            RawCategory(categoryId = CustomExpenseCategoriesIds.GAS.name, res = R.drawable.gas),
            RawCategory(categoryId = CustomExpenseCategoriesIds.PARKING.name, res = R.drawable.parking),
            RawCategory(categoryId = CustomExpenseCategoriesIds.PUBLIC_TRANSPORT.name, res = R.drawable.public_transport),
        ),
        R.string.education to listOf(
            RawCategory(categoryId = CustomExpenseCategoriesIds.COURSES.name, res = R.drawable.courses),
            RawCategory(categoryId = CustomExpenseCategoriesIds.STUDY_SUPPLIES.name, res = R.drawable.study_supplies),
            RawCategory(categoryId = CustomExpenseCategoriesIds.TUITION_FEE.name, res = R.drawable.tuition_fee)
        ),
        R.string.other to listOf(
            RawCategory(categoryId = CustomExpenseCategoriesIds.DEBT.name, res = R.drawable.debt),
            RawCategory(categoryId = CustomExpenseCategoriesIds.DONATION.name, res = R.drawable.donation),
            RawCategory(categoryId = CustomExpenseCategoriesIds.SAVINGS.name, res = R.drawable.savings),
        )
    )
}

object CustomRawIncomeCategories {
    val categories = mapOf(
        R.string.business to listOf(
            RawCategory(categoryId = CustomIncomeCategoriesIds.REVENUE.name, res = R.drawable.revenue),
            RawCategory(categoryId = CustomIncomeCategoriesIds.SERVICE_FEES.name,res =  R.drawable.service_fees),
        ),
        R.string.employment to listOf(
            RawCategory(categoryId = CustomIncomeCategoriesIds.BONUS.name, res = R.drawable.bonus),
            RawCategory(categoryId = CustomIncomeCategoriesIds.FREELANCE.name, res = R.drawable.freelance),
            RawCategory(categoryId = CustomIncomeCategoriesIds.OVERTIME.name, res = R.drawable.overtime),
            RawCategory(categoryId = CustomIncomeCategoriesIds.TIP.name, res = R.drawable.tip),
        ),
        R.string.investment to listOf(
            RawCategory(categoryId = CustomIncomeCategoriesIds.CAPITAL.name, res = R.drawable.capital),
            RawCategory(categoryId = CustomIncomeCategoriesIds.DIVIDENDS.name, res = R.drawable.dividends),
            RawCategory(categoryId = CustomIncomeCategoriesIds.RENTAL_INCOME.name, res = R.drawable.rental_income),
        ),
        R.string.other to listOf(
            RawCategory(categoryId = CustomIncomeCategoriesIds.AWARD.name, res = R.drawable.award),
            RawCategory(categoryId = CustomIncomeCategoriesIds.CHILD_SUPPORT.name, res = R.drawable.child_support),
            RawCategory(categoryId = CustomIncomeCategoriesIds.INHERITANCE.name, res = R.drawable.inheritance),
            RawCategory(categoryId = CustomIncomeCategoriesIds.PENSION.name, res = R.drawable.pension),
        )
    )
}

enum class CustomExpenseCategoriesIds {
    // Entertainment
    MOVIE, STREAMING_SERVICE, VIDEO_GAME,
    // Food
    FOOD_DELIVERY, RESTAURANT,
    // Health
    DOCTOR, HEALTH_INSURANCE, MENTAL_HEALTH, PRESCRIPTION,
    // Housing
    HOUSE_MAINTENANCE, PROPERTY_TAX, RENT, RENTERS_INSURANCE,
    // Personal care
    CLOTHING, COSMETICS_SKINCARE, FITNESS, HAIRCUT_STYLING,
    // Transportation
    CAR_INSURANCE, CAR_REPAIR, GAS, PARKING, PUBLIC_TRANSPORT,
    // Education
    COURSES, STUDY_SUPPLIES, TUITION_FEE,
    // Other
    DEBT, DONATION, SAVINGS
}
enum class CustomIncomeCategoriesIds {
    // Business
    REVENUE, SERVICE_FEES,
    // Employment
    BONUS, FREELANCE, OVERTIME, TIP,
    // Investment
    CAPITAL, DIVIDENDS, RENTAL_INCOME,
    // Other
    AWARD, CHILD_SUPPORT, INHERITANCE, PENSION
}

const val firestoreExpenseCategories = "customExpenseCategories"
const val firestoreIncomeCategories = "customIncomeCategories"