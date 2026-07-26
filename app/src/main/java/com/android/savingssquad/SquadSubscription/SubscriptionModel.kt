package com.android.savingssquad.SquadSubscription

import com.google.firebase.Timestamp

data class SubscriptionModel(

    var plan: Plan = Plan.FREE,
    var billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,   // ⭐ NEW
    var loanAddon: Boolean = false,

    var isTrialActive: Boolean = true,

    var trialStartDate: Timestamp = Timestamp.now(),
    var trialEndDate: Timestamp = Timestamp.now(),
    var trialDays: Int = 45,

    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now(),

    var maxMembers: Int = 10,
    var features: Features = Features()
) {

    enum class Plan(val value: String) {
        FREE("FREE"),
        BASIC("BASIC"),
        BUSINESS("BUSINESS")
    }

    // ⭐ NEW — mirrors iOS SubscriptionModel.BillingPeriod
    enum class BillingPeriod(val value: String) {
        MONTHLY("MONTHLY"),
        SIX_MONTH("SIX_MONTH"),
        YEARLY("YEARLY");

        val displayName: String
            get() = when (this) {
                MONTHLY -> "Monthly"
                SIX_MONTH -> "6 Months"
                YEARLY -> "Yearly"
            }

        companion object {
            fun fromValue(value: String?): BillingPeriod =
                entries.firstOrNull { it.value == value } ?: MONTHLY
        }
    }

    data class Features(
        var contribution: Boolean = true,
        var loan: Boolean = false
    )
}

// ⭐ NEW — per-period pricing block, reused for BASIC and BUSINESS
// NOTE: every param needs a default value, or Firestore's POJO mapper can't
// synthesize a no-arg constructor for this nested object and deserialization
// throws "does not define a no-argument constructor".
data class PlanPricing(
    var monthly: Int = 0,
    var sixMonth: Int = 0,
    var yearly: Int = 0,

    var monthlyText: String = "",
    var sixMonthText: String = "",
    var yearlyText: String = "",

    // Optional badges like "Save 16%" shown next to the period picker
    var sixMonthSavingsText: String = "",
    var yearlySavingsText: String = ""
) {

    fun price(period: SubscriptionModel.BillingPeriod): Int =
        when (period) {
            SubscriptionModel.BillingPeriod.MONTHLY -> monthly
            SubscriptionModel.BillingPeriod.SIX_MONTH -> sixMonth
            SubscriptionModel.BillingPeriod.YEARLY -> yearly
        }

    fun priceText(period: SubscriptionModel.BillingPeriod): String =
        when (period) {
            SubscriptionModel.BillingPeriod.MONTHLY -> monthlyText
            SubscriptionModel.BillingPeriod.SIX_MONTH -> sixMonthText
            SubscriptionModel.BillingPeriod.YEARLY -> yearlyText
        }

    fun savingsText(period: SubscriptionModel.BillingPeriod): String? =
        when (period) {
            SubscriptionModel.BillingPeriod.MONTHLY -> null
            SubscriptionModel.BillingPeriod.SIX_MONTH -> sixMonthSavingsText.ifEmpty { null }
            SubscriptionModel.BillingPeriod.YEARLY -> yearlySavingsText.ifEmpty { null }
        }
}

data class RemoteConfig(

    // Trial
    var trialDays: Int = 45,

    // Free (no billing period — always free)
    var free_maxMembers: Int = 10,
    var free_contribution: Boolean = true,
    var free_loan: Boolean = false,
    var free_priceText: String = "Free",
    var free_tagline: String = "Perfect for families and small squads",

    // Basic
    var basic_maxMembers: Int = 50,
    var basic_contribution: Boolean = true,
    var basic_loan: Boolean = false,
    var basic_tagline: String = "Ideal for growing squads with more members",

    // ⭐ NEW — replaces basic_price / basic_priceText
    var basic_pricing: PlanPricing = PlanPricing(
        monthly = 99, sixMonth = 499, yearly = 899,
        monthlyText = "₹99/month", sixMonthText = "₹499/6 months", yearlyText = "₹899/year",
        sixMonthSavingsText = "Save 16%", yearlySavingsText = "Save 24%"
    ),

    // Business
    var biz_maxMembers: Int = 200,
    var biz_contribution: Boolean = true,
    var biz_loan: Boolean = true,
    var biz_tagline: String = "Complete solution for large squads and organizations",

    // ⭐ NEW — replaces biz_price / biz_priceText
    var biz_pricing: PlanPricing = PlanPricing(
        monthly = 199, sixMonth = 999, yearly = 1799,
        monthlyText = "₹199/month", sixMonthText = "₹999/6 months", yearlyText = "₹1799/year",
        sixMonthSavingsText = "Save 16%", yearlySavingsText = "Save 25%"
    ),

    // Loan Add-on (kept monthly-only for now)
    var addon_loan_enabled: Boolean = true,
    var addon_loan_price: Int = 49,
    var addon_loan_priceText: String = "₹49/month",
    var addon_loan_tagline: String = "Add loan management to your FREE or BASIC plan"
) {

    fun maxMembers(plan: SubscriptionModel.Plan): Int =
        when (plan) {
            SubscriptionModel.Plan.FREE -> free_maxMembers
            SubscriptionModel.Plan.BASIC -> basic_maxMembers
            SubscriptionModel.Plan.BUSINESS -> biz_maxMembers
        }

    fun features(plan: SubscriptionModel.Plan): SubscriptionModel.Features =
        when (plan) {
            SubscriptionModel.Plan.FREE ->
                SubscriptionModel.Features(
                    contribution = free_contribution,
                    loan = free_loan
                )

            SubscriptionModel.Plan.BASIC ->
                SubscriptionModel.Features(
                    contribution = basic_contribution,
                    loan = basic_loan
                )

            SubscriptionModel.Plan.BUSINESS ->
                SubscriptionModel.Features(
                    contribution = biz_contribution,
                    loan = biz_loan
                )
        }

    // ⭐ NEW — price now needs a period. Free plan ignores it.
    fun price(plan: SubscriptionModel.Plan, period: SubscriptionModel.BillingPeriod): Int =
        when (plan) {
            SubscriptionModel.Plan.FREE -> 0
            SubscriptionModel.Plan.BASIC -> basic_pricing.price(period)
            SubscriptionModel.Plan.BUSINESS -> biz_pricing.price(period)
        }

    fun priceText(plan: SubscriptionModel.Plan, period: SubscriptionModel.BillingPeriod): String =
        when (plan) {
            SubscriptionModel.Plan.FREE -> free_priceText
            SubscriptionModel.Plan.BASIC -> basic_pricing.priceText(period)
            SubscriptionModel.Plan.BUSINESS -> biz_pricing.priceText(period)
        }

    fun savingsText(plan: SubscriptionModel.Plan, period: SubscriptionModel.BillingPeriod): String? =
        when (plan) {
            SubscriptionModel.Plan.FREE -> null
            SubscriptionModel.Plan.BASIC -> basic_pricing.savingsText(period)
            SubscriptionModel.Plan.BUSINESS -> biz_pricing.savingsText(period)
        }

    fun tagline(plan: SubscriptionModel.Plan): String =
        when (plan) {
            SubscriptionModel.Plan.FREE -> free_tagline
            SubscriptionModel.Plan.BASIC -> basic_tagline
            SubscriptionModel.Plan.BUSINESS -> biz_tagline
        }
}