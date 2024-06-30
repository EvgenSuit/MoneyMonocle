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
    val category: String = "",
    @DrawableRes val res: Int? = null,
    @StringRes val name: Int? = null
)



val defaultRawExpenseCategories = listOf(
    RawCategory(category = DefaultExpenseCategoriesIds.ENTERTAINMENT.name,
        res = R.drawable.entertainment, name = R.string.entertainment
    ),
    RawCategory(category = DefaultExpenseCategoriesIds.GROCERIES.name, res = R.drawable.groceries, name = R.string.groceries),
    RawCategory(category = DefaultExpenseCategoriesIds.INSURANCE.name, res = R.drawable.insurance, name = R.string.insurance),
    RawCategory(category = DefaultExpenseCategoriesIds.TRANSPORTATION.name, res = R.drawable.transportation,name =  R.string.transportation),
    RawCategory(category = DefaultExpenseCategoriesIds.UTILITIES.name, res = R.drawable.utilities,name =  R.string.utilities),
)
val defaultRawIncomeCategories = listOf(
    RawCategory(category = DefaultIncomeCategoriesIds.WAGE.name, res = R.drawable.wage,name =  R.string.wage),
    RawCategory(category = DefaultIncomeCategoriesIds.BUSINESS.name, res = R.drawable.business,name =  R.string.business),
    RawCategory(category = DefaultIncomeCategoriesIds.INTEREST.name, res = R.drawable.interest,name =  R.string.interest),
    RawCategory(category = DefaultIncomeCategoriesIds.INVESTMENT.name, res = R.drawable.investment,name =  R.string.investment),
    RawCategory(category = DefaultIncomeCategoriesIds.GIFT.name, res = R.drawable.gift,name =  R.string.gift),
    RawCategory(category = DefaultIncomeCategoriesIds.GOVERNMENT_PAYMENT.name, res = R.drawable.government_payment,name =  R.string.government_payment),
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
            RawCategory(category = CustomExpenseCategoriesIds.MOVIE.name, res = R.drawable.movie),
            RawCategory(category = CustomExpenseCategoriesIds.STREAMING_SERVICE.name, res = R.drawable.streaming_service),
            RawCategory(category = CustomExpenseCategoriesIds.VIDEO_GAME.name, res = R.drawable.video_game),
        ),
        R.string.food to listOf(
            RawCategory(category = CustomExpenseCategoriesIds.FOOD_DELIVERY.name, res = R.drawable.food_delivery),
            RawCategory(category = CustomExpenseCategoriesIds.RESTAURANT.name, res = R.drawable.restaurant),
        ),
        R.string.health to listOf(
            RawCategory(category = CustomExpenseCategoriesIds.DOCTOR.name, res = R.drawable.doctor),
            RawCategory(category = CustomExpenseCategoriesIds.HEALTH_INSURANCE.name, res = R.drawable.health_insurance),
            RawCategory(category = CustomExpenseCategoriesIds.MENTAL_HEALTH.name, res = R.drawable.mental_health),
            RawCategory(category = CustomExpenseCategoriesIds.PRESCRIPTION.name, res = R.drawable.prescription)
        ),
        R.string.housing to listOf(
            RawCategory(category = CustomExpenseCategoriesIds.HOUSE_MAINTENANCE.name, res = R.drawable.house_maintenance),
            RawCategory(category = CustomExpenseCategoriesIds.PROPERTY_TAX.name, res = R.drawable.property_tax),
            RawCategory(category = CustomExpenseCategoriesIds.RENT.name, res = R.drawable.rent),
            RawCategory(category = CustomExpenseCategoriesIds.RENTERS_INSURANCE.name, res = R.drawable.renters_insurance)
        ),
        R.string.personal_care to listOf(
            RawCategory(category = CustomExpenseCategoriesIds.CLOTHING.name, res = R.drawable.clothing),
            RawCategory(category = CustomExpenseCategoriesIds.COSMETICS_SKINCARE.name, res = R.drawable.cosmetics_skincare),
            RawCategory(category = CustomExpenseCategoriesIds.FITNESS.name, res = R.drawable.fitness),
            RawCategory(category = CustomExpenseCategoriesIds.HAIRCUT_STYLING.name, res = R.drawable.haircut_styling)
        ),
        R.string.transportation to listOf(
            RawCategory(category = CustomExpenseCategoriesIds.CAR_INSURANCE.name, res = R.drawable.car_insurance),
            RawCategory(category = CustomExpenseCategoriesIds.CAR_REPAIR.name, res = R.drawable.car_repair),
            RawCategory(category = CustomExpenseCategoriesIds.GAS.name, res = R.drawable.gas),
            RawCategory(category = CustomExpenseCategoriesIds.PARKING.name, res = R.drawable.parking),
            RawCategory(category = CustomExpenseCategoriesIds.PUBLIC_TRANSPORT.name, res = R.drawable.public_transport),
        ),
        R.string.education to listOf(
            RawCategory(category = CustomExpenseCategoriesIds.COURSES.name, res = R.drawable.courses),
            RawCategory(category = CustomExpenseCategoriesIds.STUDY_SUPPLIES.name, res = R.drawable.study_supplies),
            RawCategory(category = CustomExpenseCategoriesIds.TUITION_FEE.name, res = R.drawable.tuition_fee)
        ),
        R.string.other to listOf(
            RawCategory(category = CustomExpenseCategoriesIds.DEBT.name, res = R.drawable.debt),
            RawCategory(category = CustomExpenseCategoriesIds.DONATION.name, res = R.drawable.donation),
            RawCategory(category = CustomExpenseCategoriesIds.SAVINGS.name, res = R.drawable.savings),
        )
    )
}

object CustomRawIncomeCategories {
    val categories = mapOf(
        R.string.business to listOf(
            RawCategory(category = CustomIncomeCategoriesIds.REVENUE.name, res = R.drawable.revenue),
            RawCategory(category = CustomIncomeCategoriesIds.SERVICE_FEES.name,res =  R.drawable.service_fees),
        ),
        R.string.employment to listOf(
            RawCategory(category = CustomIncomeCategoriesIds.BONUS.name, res = R.drawable.bonus),
            RawCategory(category = CustomIncomeCategoriesIds.FREELANCE.name, res = R.drawable.freelance),
            RawCategory(category = CustomIncomeCategoriesIds.OVERTIME.name, res = R.drawable.overtime),
            RawCategory(category = CustomIncomeCategoriesIds.TIP.name, res = R.drawable.tip),
        ),
        R.string.investment to listOf(
            RawCategory(category = CustomIncomeCategoriesIds.CAPITAL.name, res = R.drawable.capital),
            RawCategory(category = CustomIncomeCategoriesIds.DIVIDENDS.name, res = R.drawable.dividends),
            RawCategory(category = CustomIncomeCategoriesIds.RENTAL_INCOME.name, res = R.drawable.rental_income),
        ),
        R.string.other to listOf(
            RawCategory(category = CustomIncomeCategoriesIds.AWARD.name, res = R.drawable.award),
            RawCategory(category = CustomIncomeCategoriesIds.CHILD_SUPPORT.name, res = R.drawable.child_support),
            RawCategory(category = CustomIncomeCategoriesIds.INHERITANCE.name, res = R.drawable.inheritance),
            RawCategory(category = CustomIncomeCategoriesIds.PENSION.name, res = R.drawable.pension),
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