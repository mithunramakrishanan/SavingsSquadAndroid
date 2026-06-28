package com.android.savingssquad.SquadSubscription

import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import com.android.savingssquad.singleton.Plan
data class SubscriptionModel(

    var plan: Plan = Plan.FREE,
    var loanAddon: Boolean = false,
    var isTrialActive: Boolean = true,

    var trialStartDate: Timestamp = Timestamp.now(),
    var trialEndDate: Timestamp = Timestamp.now(),
    var trialDays: Int = 30,

    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now(),

    // runtime only (not stored)
    var maxMembers: Int = 25,
    var features: Features = Features()
) {

    data class Features(
        var contribution: Boolean = true,
        var loan: Boolean = false
    )
}


data class RemoteConfig(

    // MARK: - TRIAL
    var trialDays: Int = 30,

    // MARK: - FREE PLAN
    var free_maxMembers: Int = 10,
    var free_contribution: Boolean = true,
    var free_loan: Boolean = false,
    var free_price: String = "₹0/month",
    var free_tagline: String = "Perfect for small squads getting started",

    // MARK: - BASIC PLAN
    var basic_maxMembers: Int = 50,
    var basic_contribution: Boolean = true,
    var basic_loan: Boolean = false,
    var basic_price: String = "₹119/month",
    var basic_tagline: String = "For growing squads that need more capacity",

    // MARK: - BUSINESS PLAN
    var biz_maxMembers: Int = 200,
    var biz_contribution: Boolean = true,
    var biz_loan: Boolean = true,
    var biz_price: String = "₹299/month",
    var biz_tagline: String = "For large squads and financial operations",

    // MARK: - LOAN ADDON
    var addon_loan_enabled: Boolean = true,
    var addon_loan_price: String = "₹49/month",
    var addon_loan_tagline: String = "Available with FREE or BASIC plans"
) {

    fun maxMembers(plan: Plan): Int {
        return when (plan) {
            Plan.FREE -> free_maxMembers
            Plan.BASIC -> basic_maxMembers
            Plan.BUSINESS -> biz_maxMembers
        }
    }

    fun features(plan: Plan): SubscriptionModel.Features {
        return when (plan) {

            Plan.FREE ->
                SubscriptionModel.Features(free_contribution, free_loan)

            Plan.BASIC ->
                SubscriptionModel.Features(basic_contribution, basic_loan)

            Plan.BUSINESS ->
                SubscriptionModel.Features(biz_contribution, biz_loan)
        }
    }

    fun price(plan: Plan): String {
        return when (plan) {
          Plan.FREE -> free_price
            Plan.BASIC -> basic_price
            Plan.BUSINESS -> biz_price
        }
    }

    fun tagline(plan: Plan): String {
        return when (plan) {
            Plan.FREE -> free_tagline
            Plan.BASIC -> basic_tagline
            Plan.BUSINESS -> biz_tagline
        }
    }
}

object SubscriptionConfig {

    fun createTrialDates(trialDays: Int): Pair<Timestamp, Timestamp> {

        val start = Date()

        val calendar = Calendar.getInstance()
        calendar.time = start
        calendar.add(Calendar.DAY_OF_YEAR, trialDays)

        val end = calendar.time

        return Pair(
            Timestamp(start),
            Timestamp(end)
        )
    }
}