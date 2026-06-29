package com.android.savingssquad.SquadSubscription

import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
data class SubscriptionModel(

    var plan: Plan = Plan.FREE,
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

    data class Features(
        var contribution: Boolean = true,
        var loan: Boolean = false
    )
}


data class RemoteConfig(

    // Trial
    var trialDays: Int = 45,

    // Free
    var free_maxMembers: Int = 10,
    var free_contribution: Boolean = true,
    var free_loan: Boolean = false,
    var free_price: Int = 0,
    var free_priceText: String = "Free",
    var free_tagline: String = "Perfect for families and small squads",

    // Basic
    var basic_maxMembers: Int = 50,
    var basic_contribution: Boolean = true,
    var basic_loan: Boolean = false,
    var basic_price: Int = 99,
    var basic_priceText: String = "₹99/month",
    var basic_tagline: String = "Ideal for growing squads with more members",

    // Business
    var biz_maxMembers: Int = 200,
    var biz_contribution: Boolean = true,
    var biz_loan: Boolean = true,
    var biz_price: Int = 199,
    var biz_priceText: String = "₹199/month",
    var biz_tagline: String = "Complete solution for large squads and organizations",

    // Loan Add-on
    var addon_loan_enabled: Boolean = true,
    var addon_loan_price: Int = 49,
    var addon_loan_priceText: String = "49/month",
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

    fun price(plan: SubscriptionModel.Plan): Int =
        when (plan) {
            SubscriptionModel.Plan.FREE -> free_price
            SubscriptionModel.Plan.BASIC -> basic_price
            SubscriptionModel.Plan.BUSINESS -> biz_price
        }

    fun priceText(plan: SubscriptionModel.Plan): String =
        when (plan) {
            SubscriptionModel.Plan.FREE -> free_priceText
            SubscriptionModel.Plan.BASIC -> basic_priceText
            SubscriptionModel.Plan.BUSINESS -> biz_priceText
        }

    fun tagline(plan: SubscriptionModel.Plan): String =
        when (plan) {
            SubscriptionModel.Plan.FREE -> free_tagline
            SubscriptionModel.Plan.BASIC -> basic_tagline
            SubscriptionModel.Plan.BUSINESS -> biz_tagline
        }
}