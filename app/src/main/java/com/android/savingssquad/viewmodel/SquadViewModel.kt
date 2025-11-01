package com.android.savingssquad.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.model.Login
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.model.GroupFundActivity
import com.android.savingssquad.model.EMIConfiguration
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.GroupFundRule
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.PayoutStatus
import com.android.savingssquad.model.pendingInstallments
import com.android.savingssquad.model.pendingLoans
import com.android.savingssquad.singleton.AlertType
import com.android.savingssquad.singleton.CashfreePaymentAction
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.GroupFundActivityType
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.orNow
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.view.MemberTabView
import com.android.savingssquad.view.ManagerTabView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.concurrent.CountDownLatch

class SquadViewModel : ViewModel() {

    // ------------------------------------------------------------------------
    // 🔹 Firestore + Initialization
    // ------------------------------------------------------------------------
    private val manager = FirestoreManager.shared
    private var groupFundID: String = ""
    var loginMember: Login? = null
    private var groupFundListener: ListenerRegistration? = null

    // ------------------------------------------------------------------------
    // 🔹 GroupFund
    // ------------------------------------------------------------------------
    private val _groupFund = MutableStateFlow<GroupFund?>(null)
    val groupFund: StateFlow<GroupFund?> = _groupFund
    fun setGroupFund(value: GroupFund?) { _groupFund.value = value }

    private val _rules = MutableStateFlow<List<GroupFundRule>>(emptyList())
    val rules: StateFlow<List<GroupFundRule>> = _rules
    fun setRules(list: List<GroupFundRule>) { _rules.value = list }

    private val _groupFundActivities = MutableStateFlow<List<GroupFundActivity>>(emptyList())
    val groupFundActivities: StateFlow<List<GroupFundActivity>> = _groupFundActivities
    fun setGroupFundActivities(list: List<GroupFundActivity>) { _groupFundActivities.value = list }

    private val _groupFundPayments = MutableStateFlow<List<PaymentsDetails>>(emptyList())
    val groupFundPayments: StateFlow<List<PaymentsDetails>> = _groupFundPayments
    fun setGroupFundPayments(list: List<PaymentsDetails>) { _groupFundPayments.value = list }

    private val _remainingMonths = MutableStateFlow(0)
    val remainingMonths: StateFlow<Int> = _remainingMonths
    fun setRemainingMonths(value: Int) { _remainingMonths.value = value }

    // ------------------------------------------------------------------------
    // 🔹 Members & Contributions
    // ------------------------------------------------------------------------
    private val _users = MutableStateFlow<List<Login>>(emptyList())
    val users: StateFlow<List<Login>> = _users
    fun setUsers(list: List<Login>) { _users.value = list }

    private val _selectedUser = MutableStateFlow<Login?>(null)
    val selectedUser: StateFlow<Login?> = _selectedUser
    fun setSelectedUser(value: Login?) { _selectedUser.value = value }

    private val _groupFundMembers = MutableStateFlow<List<Member>>(emptyList())
    val groupFundMembers: StateFlow<List<Member>> = _groupFundMembers
    fun setGroupFundMembers(list: List<Member>) {
        _groupFundMembers.value = list
        _groupFundMembersCount.value = list.size
    }

    private val _groupFundMembersCount = MutableStateFlow(0)
    val groupFundMembersCount: StateFlow<Int> = _groupFundMembersCount
    fun setGroupFundMembersCount(value: Int) { _groupFundMembersCount.value = value }

    private val _memberDetail = MutableStateFlow<Member?>(null)
    val memberDetail: StateFlow<Member?> = _memberDetail
    fun setMemberDetail(value: Member?) { _memberDetail.value = value }

    private val _currentMember = MutableStateFlow<Member?>(null)
    val currentMember: StateFlow<Member?> = _currentMember
    fun setCurrentMember(value: Member?) { _currentMember.value = value }

    private val _selectedContributions = MutableStateFlow<List<ContributionDetail>>(emptyList())
    val selectedContributions: StateFlow<List<ContributionDetail>> = _selectedContributions
    fun setSelectedContributions(list: List<ContributionDetail>) { _selectedContributions.value = list }

    private val _groupFundMemberNames = MutableStateFlow<List<String>>(emptyList())
    val groupFundMemberNames: StateFlow<List<String>> = _groupFundMemberNames
    fun setGroupFundMemberNames(list: List<String>) { _groupFundMemberNames.value = list }

    private val _totalContributionMember = MutableStateFlow(0.0)
    val totalContributionMember: StateFlow<Double> = _totalContributionMember
    fun setTotalContributionMember(value: Double) { _totalContributionMember.value = value }

    private val _upcomingPayment = MutableStateFlow("")
    val upcomingPayment: StateFlow<String> = _upcomingPayment
    fun setUpcomingPayment(value: String) { _upcomingPayment.value = value }

    private val _isFetchingMembers = MutableStateFlow(true)
    val isFetchingMembers: StateFlow<Boolean> = _isFetchingMembers
    fun setIsFetchingMembers(value: Boolean) { _isFetchingMembers.value = value }

    private val _isFetchingTotalAmountCollected = MutableStateFlow(true)
    val isFetchingTotalAmountCollected: StateFlow<Boolean> = _isFetchingTotalAmountCollected
    fun setIsFetchingTotalAmountCollected(value: Boolean) { _isFetchingTotalAmountCollected.value = value }

    // ------------------------------------------------------------------------
    // 🔹 EMI Configurations
    // ------------------------------------------------------------------------
    private val _emiConfigurations = MutableStateFlow<List<EMIConfiguration>>(emptyList())
    val emiConfigurations: StateFlow<List<EMIConfiguration>> = _emiConfigurations
    fun setEMIConfigurations(list: List<EMIConfiguration>) { _emiConfigurations.value = list }

    // ------------------------------------------------------------------------
    // 🔹 Loans
    // ------------------------------------------------------------------------
    private val _memberLoans = MutableStateFlow<List<MemberLoan>>(emptyList())
    val memberLoans: StateFlow<List<MemberLoan>> = _memberLoans
    fun setMemberLoans(list: List<MemberLoan>) { _memberLoans.value = list }

    private val _memberPendingLoans = MutableStateFlow<List<MemberLoan>?>(null)
    val memberPendingLoans: StateFlow<List<MemberLoan>?> = _memberPendingLoans
    fun setMemberPendingLoans(list: List<MemberLoan>?) { _memberPendingLoans.value = list }

    private val _selectedLoan = MutableStateFlow<MemberLoan?>(null)
    val selectedLoan: StateFlow<MemberLoan?> = _selectedLoan
    fun setSelectedLoan(value: MemberLoan?) { _selectedLoan.value = value }

    private val _isPendingLoanAvailable = MutableStateFlow(false)
    val isPendingLoanAvailable: StateFlow<Boolean> = _isPendingLoanAvailable
    fun setIsPendingLoanAvailable(value: Boolean) { _isPendingLoanAvailable.value = value }

    // ------------------------------------------------------------------------
    // 🔹 UI States / Popups
    // ------------------------------------------------------------------------
    private val _showPopup = MutableStateFlow(false)
    val showPopup: StateFlow<Boolean> = _showPopup
    fun setShowPopup(value: Boolean) { _showPopup.value = value }

    private val _showAddMemberPopup = MutableStateFlow(false)
    val showAddMemberPopup: StateFlow<Boolean> = _showAddMemberPopup
    fun setShowAddMemberPopup(value: Boolean) { _showAddMemberPopup.value = value }

    private val _showUpdateMemberPopup = MutableStateFlow(false)
    val showUpdateMemberPopup: StateFlow<Boolean> = _showUpdateMemberPopup
    fun setShowUpdateMemberPopup(value: Boolean) { _showUpdateMemberPopup.value = value }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    fun setErrorMessage(value: String?) { _errorMessage.value = value }

    // ------------------------------------------------------------------------
    // 🔹 Payment
    // ------------------------------------------------------------------------
    private val _showPayment = MutableStateFlow(false)
    val showPayment: StateFlow<Boolean> = _showPayment
    fun setShowPayment(value: Boolean) { _showPayment.value = value }

    private val _paymentOrderId = MutableStateFlow("")
    val paymentOrderId: StateFlow<String> = _paymentOrderId
    fun setPaymentOrderId(value: String) { _paymentOrderId.value = value }

    private val _paymentOrderToken = MutableStateFlow("")
    val paymentOrderToken: StateFlow<String> = _paymentOrderToken
    fun setPaymentOrderToken(value: String) { _paymentOrderToken.value = value }

    init {
        val login = UserDefaultsManager.getLogin()
        if (login != null) {
            groupFundID = login.groupFundID.toString()
            loginMember = login
            _selectedUser.value = login
        } else {
            val defaultLogin = Login(
                groupFundID = "",
                groupFundName = "",
                groupFundUsername = "",
                groupFundUserId = "",
                phoneNumber = "",
                role = GroupFundUserType.GROUP_FUND_MANAGER.value,
                groupFundCreatedDate = Date().asTimestamp,
                userCreatedDate = Date().asTimestamp
            )
            groupFundID = ""
            loginMember = defaultLogin
            _selectedUser.value = defaultLogin
        }

        observeGroupFundChanges()
    }

    override fun onCleared() {
        super.onCleared()
        groupFundListener?.remove()
    }

    private fun observeGroupFundChanges() {
        if (groupFundID.isEmpty()) return

        groupFundListener?.remove()
        val db = FirebaseFirestore.getInstance()

        groupFundListener = db.collection("groupFunds")
            .document(groupFundID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("❌ Failed to observe groupFund changes: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    println("⚠️ GroupFund document deleted or doesn't exist")
                    return@addSnapshotListener
                }
                println("🔄 GroupFund updated remotely, refetching data...")
                fetchGroupFundByID(showLoader = false) { success, _, _ ->
                    println(if (success) "✅ GroupFund re-fetched on update" else "❌ Re-fetch failed")
                }
            }
    }

    fun addUserLogin(showLoader: Boolean = true, member: Member) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        val login = Login(
            groupFundID = member.groupFundID,
            groupFundName = groupFund.value?.groupFundName ?: "",
            groupFundUsername = member.name,
            groupFundUserId = member.id ?: "",
            phoneNumber = member.phoneNumber,
            role = GroupFundUserType.GROUP_FUND_MEMBER.value,
            groupFundCreatedDate = Date().asTimestamp,
            userCreatedDate = Date().asTimestamp
        )

        manager.addUserLogin(login) { success, error ->
            viewModelScope.launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    if (login.getRoleEnum() == GroupFundUserType.GROUP_FUND_MEMBER) {
                        createContributionWhenMemberCreate(member)
                    }
                } else {
                    handleFetchError(error ?: "Unknown error") {
                        addUserLogin(showLoader, member)
                    }
                }
            }
        }
    }

    fun fetchUserLogins(
        showLoader: Boolean,
        phoneNumber: String,
        completion: (Boolean, String?) -> Unit
    ) {
        // ✅ Check Internet
        if (!CommonFunctions.isInternetAvailable()) {

            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.fetchUserLogins(phoneNumber) { loginList, error ->

            // DispatchQueue.main.async → main-safe via viewModelScope
            viewModelScope.launch {

                // Guard self check not needed in Kotlin, directly use `this@SquadViewModel`
                try {
                    if (showLoader) {
                        LoaderManager.shared.hideLoader()
                    }

                    if (loginList != null && loginList.isNotEmpty()) {
                        _users.value = loginList
                        _showPopup.value = loginList.size > 1

                        if (loginList.size == 1) {
                            val selectedUser = loginList.first()
                            selectedUser?.let {
                                _selectedUser.value = it
                                UserDefaultsManager.saveLogin(it)
                                UserDefaultsManager.saveIsLoggedIn(true)

//                                if (it.role == GroupFundUserType.GROUP_FUND_MANAGER) {
//                                    CommonFunctions.replaceRootView(ManagerTabView())
//                                } else {
//                                    CommonFunctions.replaceRootView(MemberTabView())
//                                }
                            }
                        }

                        completion(true, null)

                    } else {
                        val errorMessage = error ?: "No groupFunds found for this user"
                        handleFetchError(errorMessage) { }
                        completion(false, errorMessage)
                    }

                } catch (e: Exception) {
                    if (showLoader) {
                        LoaderManager.shared.hideLoader()
                    }
                    completion(false, e.localizedMessage)
                }
            }
        }
    }

    fun fetchGroupFundByID(
        showLoader: Boolean,
        completion: (Boolean, GroupFund?, String?) -> Unit
    ) {
        // ✅ Internet check
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        UserDefaultsManager.getLogin()?.let { login ->
            this.groupFundID = login.groupFundID.toString()
            this.loginMember = login
            _selectedUser.value = login
        }

        manager.fetchGroupFundByID(groupFundID) { fetchedGroupFund, error ->
            viewModelScope.launch {
                try {
                    if (fetchedGroupFund == null) {
                        if (showLoader) LoaderManager.shared.hideLoader()
                        handleFetchError(error ?: "Failed to fetch groupFund details") {
                            fetchGroupFundByID(showLoader, completion)
                        }
                        completion(false, null, error ?: "Failed to fetch groupFund details")
                        return@launch
                    }

                    _groupFund.value = fetchedGroupFund
                    _remainingMonths.value = CommonFunctions.getRemainingMonths(
                        startDate = Date(),
                        endDate = fetchedGroupFund.groupFundEndDate?.toDate() ?: Date()
                    )

                    _isFetchingTotalAmountCollected.value = true

                    // 🔹 Parallel background work with coroutine async blocks
                    val fetchEMI = async { fetchEMIConfigurations(true) { _, _ -> } }
                    val fetchMembers = async { fetchMembers(false) { _, _, _ -> } }
                    val fetchPayments = async {
                        manager.fetchPayments(groupFundID) { fetchedPayments, err ->
                            viewModelScope.launch {
                                if (fetchedPayments != null) {
                                    _groupFundPayments.value = fetchedPayments
                                    _isFetchingTotalAmountCollected.value = false
                                } else {
                                    val errorMsg = err ?: "❌ Failed to fetch payments"
                                    println(errorMsg)
                                    handleFetchError(errorMsg) {
                                        fetchPayments(false) { success, errorMsg ->
                                            println(if (success) "✅ Payments fetched successfully" else "❌ Error: ${errorMsg ?: "Unknown error"}")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    fetchEMI.await()
                    fetchMembers.await()
                    fetchPayments.await()

                    if (showLoader) LoaderManager.shared.hideLoader()
                    println("✅ All background tasks completed")

                    completion(true, fetchedGroupFund, null)
                } catch (e: Exception) {
                    if (showLoader) LoaderManager.shared.hideLoader()
                    completion(false, null, e.localizedMessage)
                }
            }
        }
    }


    fun updateGroupFund(
        showLoader: Boolean = true,
        groupFund: GroupFund,
        completion: (Boolean, GroupFund?, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.updateGroupFund(groupFund) { success, updatedGroupFund, errorMessage ->
            viewModelScope.launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success && updatedGroupFund != null) {
                    println("✅ GroupFund updated successfully!")
                    _groupFund.value = updatedGroupFund
                    _remainingMonths.value = CommonFunctions.getRemainingMonths(
                        startDate = Date(),
                        endDate = updatedGroupFund.groupFundEndDate?.orNow ?: Date()
                    )
                    completion(true, updatedGroupFund, null)
                } else {
                    val errorMsg = errorMessage ?: "Unknown error while updating groupFund"
                    println("❌ $errorMsg")
                    handleFetchError(errorMsg) {
                        updateGroupFund(showLoader, groupFund, completion)
                    }
                    completion(false, null, errorMsg)
                }
            }
        }
    }


    fun addMember(
        showLoader: Boolean = true,
        name: String,
        phone: String,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        val groupFund = _groupFund.value
        if (groupFund == null) {
            completion(false, "Group Fund not found")
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        val newMember = Member(
            id = CommonFunctions.generateMemberID(),
            name = name,
            profileImage = "",
            mailID = "",
            phoneNumber = phone,
            password = phone,
            groupFundID = groupFund.groupFundID,
            role = GroupFundUserType.GROUP_FUND_MEMBER,
            memberCreatedDate = Date().asTimestamp,
            recordStatus = RecordStatus.ACTIVE,
            upiBeneId = "",
            bankBeneId = "",
            upiID = ""
        )

        val addAndSave: () -> Unit = {
            _showAddMemberPopup.value = false
            manager.addMember(groupFundID, newMember) { success, message ->
                viewModelScope.launch {
                    if (showLoader) LoaderManager.shared.hideLoader()

                    if (success) {
                        _groupFundMembers.value += newMember
                        _groupFundMembersCount.value += 1
                        addUserLogin(false, newMember)
                        completion(true, null)
                    } else {
                        handleFetchError(message ?: "Unknown error") {}
                        completion(false, message ?: "Unknown error")
                    }
                }
            }
        }

        if (_groupFundMembersCount.value > 0) {
            manager.fetchMembers(groupFund.groupFundID) { memberNames, error ->
                viewModelScope.launch {
                    if (error != null) {
                        if (showLoader) LoaderManager.shared.hideLoader()
                        handleFetchError(error) {
                            addMember(showLoader, name, phone, completion)
                        }
                        return@launch
                    }

                    if (memberNames != null && memberNames.map { it.name }.contains(name)) {
                        if (showLoader) LoaderManager.shared.hideLoader()
                        AlertManager.shared.showAlert(
                            title = SquadStrings.appName,
                            message = SquadStrings.nameAlreadyExists,
                            primaryButtonTitle = "OK",
                            primaryAction = {}
                        )
                        completion(false, SquadStrings.nameAlreadyExists)
                        return@launch
                    }

                    addAndSave()
                }
            }
        } else {
            addAndSave()
        }
    }

        // ✅ fetchMember
        fun fetchMember(
            showLoader: Boolean,
            groupFundID: String,
            memberID: String,
            completion: (Boolean, Member?, String?) -> Unit
        ) {
            if (!CommonFunctions.isInternetAvailable()) {
                LoaderManager.shared.hideLoader()
                AlertManager.shared.showAlert(
                    title = SquadStrings.appName,
                    message = SquadStrings.networkError,
                    primaryButtonTitle = "OK",
                    primaryAction = {}
                )
                return
            }

            if (showLoader) LoaderManager.shared.showLoader()

            viewModelScope.launch(Dispatchers.IO) { // 🧵 Run on background thread
                manager.fetchMember(groupFundID, memberID) { fetchedMember, error ->
                    // ✅ Use launch instead of withContext inside callback (no suspend issue)
                    viewModelScope.launch(Dispatchers.Main) { // 🧭 Switch back to Main thread
                        if (showLoader) LoaderManager.shared.hideLoader()

                        if (fetchedMember != null) {
                            // ✅ Update LiveData / StateFlow
                            _currentMember.value = fetchedMember
                            completion(true, fetchedMember, null)
                        } else {
                            val errorMsg = error ?: "❌ Failed to fetch member"
                            _errorMessage.value = errorMsg

                            // 🔁 Retry logic if error
                            handleFetchError(errorMsg) {
                                fetchMember(showLoader, groupFundID, memberID, completion)
                            }

                            completion(false, null, errorMsg)
                        }
                    }
                }
            }

        }

        // ✅ fetchMembers
        fun fetchMembers(showLoader: Boolean, completion: (Boolean, List<Member>?, String?) -> Unit) {
            if (!CommonFunctions.isInternetAvailable()) {
                LoaderManager.shared.hideLoader()
                AlertManager.shared.showAlert(
                    title = SquadStrings.appName,
                    message = SquadStrings.networkError,
                    primaryButtonTitle = "OK",
                    primaryAction = {}
                )
                return
            }

            if (showLoader) LoaderManager.shared.showLoader()

            viewModelScope.launch(Dispatchers.IO) { // 🧵 Run Firebase fetch in background
                manager.fetchMembers(groupFundID) { fetchedMembers, error ->
                    // ✅ Launch on Main thread safely for UI updates
                    viewModelScope.launch(Dispatchers.Main) {
                        _isFetchingMembers.value = false
                        if (showLoader) LoaderManager.shared.hideLoader()

                        when {
                            error == "No members found." -> {
                                _groupFundMembersCount.value = 0
                                completion(false, null, error)
                            }

                            fetchedMembers != null -> {
                                // ✅ Reset & update member list
                                _groupFundMembers.value = fetchedMembers
                                _groupFundMembersCount.value = fetchedMembers.size
                                _groupFundMemberNames.value = fetchedMembers.map { it.name }.toMutableList()

                                // ✅ Auto-select login member (if applicable)
                                loginMember?.groupFundUsername?.let { username ->
                                    CommonFunctions.getMemberByName(username, fetchedMembers)?.let {
                                        _memberDetail.value = it
                                    }
                                }

                                completion(true, fetchedMembers, null)
                            }

                            else -> {
                                val errorMsg = error ?: "❌ Failed to fetch members"
                                _errorMessage.value = errorMsg

                                handleFetchError(errorMsg) {
                                    fetchMembers(showLoader, completion)
                                }

                                completion(false, null, errorMsg)
                            }
                        }
                    }
                }
            }
        }

        // ✅ createContributionWhenMemberCreate
        private fun createContributionWhenMemberCreate(member: Member) {
            if (!CommonFunctions.isInternetAvailable()) {
                LoaderManager.shared.hideLoader()
                AlertManager.shared.showAlert(
                    title = SquadStrings.appName,
                    message = SquadStrings.networkError,
                    primaryButtonTitle = "OK",
                    primaryAction = {}
                )
                return
            }

            val groupFund = _groupFund.value ?: return

            manager.createContributionWhenMemberCreate(
                groupFundID = groupFund.groupFundID,
                memberID = member.id ?: "",
                memberName = member.name,
                groupFundStart = groupFund.groupFundStartDate?.toDate() ?: Date(),
                groupFundEnd = groupFund.groupFundEndDate?.toDate() ?: Date(),
                amount = groupFund.monthlyContribution
            ) { success, message ->
                if (success) {
                    createGroupFundActivity(
                        activityType = GroupFundActivityType.OTHER_ACTIVITY,
                        userName = "SQUAD MANAGER",
                        amount = 0,
                        description = "Added a new member ${member.name} to the groupFund"
                    ) {}
                } else {
                    handleFetchError(message ?: "Unknown error") {
                        createContributionWhenMemberCreate(member)
                    }
                }
            }
        }

        // ✅ contibutionEditWhenMonthsChanged
        fun contibutionEditWhenMonthsChanged(
            showLoader: Boolean,
            groupFund: GroupFund,
            groupFundEndDate: Date,
            amount: String,
            completion: (Boolean, String?) -> Unit
        ) {
            if (!CommonFunctions.isInternetAvailable()) {
                LoaderManager.shared.hideLoader()
                AlertManager.shared.showAlert(
                    title = SquadStrings.appName,
                    message = SquadStrings.networkError,
                    primaryButtonTitle = "OK",
                    primaryAction = {}
                )
                return
            }

            if (showLoader) LoaderManager.shared.showLoader()

            viewModelScope.launch(Dispatchers.IO) {
                manager.contibutionEditWhenMonthsChanged(
                    groupFundID = groupFund.groupFundID,
                    groupFundStartDate = groupFund.groupFundStartDate?.toDate() ?: Date(),
                    groupFundEndDate = groupFundEndDate,
                    amount = groupFund.monthlyContribution.toString()
                ) { success, message ->
                    // ✅ Switch back to Main thread safely
                    viewModelScope.launch(Dispatchers.Main) {
                        if (showLoader) LoaderManager.shared.hideLoader()

                        if (success) {
                            println(message ?: "✅ GroupFund contributions updated successfully!")
                            completion(true, message)
                        } else {
                            val errorMsg = message ?: "❌ Error updating groupFund contributions"
                            println(errorMsg)

                            handleFetchError(errorMsg) {
                                contibutionEditWhenMonthsChanged(
                                    showLoader = showLoader,
                                    groupFund = groupFund,
                                    groupFundEndDate = groupFundEndDate,
                                    amount = amount,
                                    completion = completion
                                )
                            }

                            completion(false, errorMsg)
                        }
                    }
                }
            }
        }

    // ✅ fetchContributionsForMember
    fun fetchContributionsForMember(
        showLoader: Boolean,
        groupFundID: String,
        memberID: String,
        completion: (List<ContributionDetail>?, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        viewModelScope.launch(Dispatchers.IO) {
            manager.fetchContributionsForMember(groupFundID, memberID) { contributions, error ->
                // ✅ Switch to main thread safely inside callback
                viewModelScope.launch(Dispatchers.Main) {
                    if (showLoader) LoaderManager.shared.hideLoader()

                    if (error != null) {
                        println(error)
                        handleFetchError(error) {
                            fetchContributionsForMember(
                                showLoader = showLoader,
                                groupFundID = groupFundID,
                                memberID = memberID,
                                completion = completion
                            )
                        }
                        completion(null, error)
                    } else {
                        println("✅ Contributions fetched successfully")
                        contributions?.let {
                            _selectedContributions.value = it
                        }
                        completion(contributions, null)
                    }
                }
            }
        }

    }

    // ✅ editContribution
    fun editContribution(
        showLoader: Boolean,
        groupFundID: String,
        memberID: String,
        contributionID: String,
        updatedContribution: ContributionDetail,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        viewModelScope.launch(Dispatchers.IO) {
            manager.editContribution(
                groupFundID = groupFundID,
                memberID = memberID,
                contributionID = contributionID,
                updatedContribution = updatedContribution
            ) { success, error ->
                // ✅ Move UI updates safely to Main thread
                viewModelScope.launch(Dispatchers.Main) {
                    if (showLoader) LoaderManager.shared.hideLoader()

                    if (success) {
                        println("✅ GroupFund contributions updated successfully!")
                        completion(true, "✅ GroupFund contributions updated successfully!")
                    } else {
                        val errorMsg = error ?: "❌ Error updating groupFund contributions"
                        println(errorMsg)

                        handleFetchError(errorMsg) {
                            editContribution(
                                showLoader = showLoader,
                                groupFundID = groupFundID,
                                memberID = memberID,
                                contributionID = contributionID,
                                updatedContribution = updatedContribution,
                                completion = completion
                            )
                        }

                        completion(false, errorMsg)
                    }
                }
            }
        }

    }

    // ✅ addGroupFundActivity
    fun addGroupFundActivity(
        showLoader: Boolean,
        activity: GroupFundActivity,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        viewModelScope.launch(Dispatchers.IO) {
            manager.addGroupFundActivity(groupFundID, activity) { success, error ->
                // ✅ Switch safely to main thread inside callback
                viewModelScope.launch(Dispatchers.Main) {
                    if (showLoader) LoaderManager.shared.hideLoader()

                    if (success) {
                        _groupFundActivities.value += activity
                        completion(true, null)
                    } else {
                        val errorMsg = error ?: "❌ Failed to append groupFund activity"
                        println(errorMsg)

                        handleFetchError(errorMsg) {
                            addGroupFundActivity(
                                showLoader = showLoader,
                                activity = activity,
                                completion = completion
                            )
                        }

                        completion(false, errorMsg)
                    }
                }
            }
        }
    }

    fun fetchGroupFundActivities(showLoader: Boolean = true, groupFundID: String) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.fetchGroupFundActivities(groupFundID) { activities, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (activities != null) {
                    _groupFundActivities.value = activities
                } else {
                    val errorMsg = error ?: "Unknown error"
                    println("❌ Error fetching groupFund activities: $errorMsg")
                    handleFetchError(errorMsg) {
                        fetchGroupFundActivities(showLoader, groupFundID)
                    }
                }
            }
        }
    }

    fun deleteGroupFundActivity(showLoader: Boolean = true, activityID: String) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.deleteGroupFundActivity(groupFundID, activityID) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    _groupFundActivities.value = _groupFundActivities.value.filter { it.groupFundID != activityID }
                } else {
                    val errorMsg = error ?: "❌ Failed to delete groupFund activity"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        deleteGroupFundActivity(showLoader, activityID)
                    }
                }
            }
        }
    }

    fun fetchRules(showLoader: Boolean = true) {
        if (!CommonFunctions.isInternetAvailable()) {
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "⚠️ No internet connection",
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.fetchGroupFundRules(groupFundID) { rules, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (rules != null) {
                    _rules.value = rules
                } else if (error != null) {
                    handleFetchError(error) {
                        fetchRules(showLoader)
                    }
                }
            }
        }
    }

    fun addRule(rule: GroupFundRule, showLoader: Boolean = true, completion: (Boolean, String?) -> Unit) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.addGroupFundRule(groupFundID, rule) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    _rules.value += rule
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to add groupFund rule"
                    handleFetchError(errorMsg) {
                        addRule(rule, showLoader, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun deleteRule(rule: GroupFundRule, showLoader: Boolean = true, completion: (Boolean, String?) -> Unit) {
        val ruleID = rule.id ?: run {
            completion(false, "❌ Invalid rule ID")
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.deleteGroupFundRule(groupFundID, ruleID) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    _rules.value = _rules.value.filter { it.id != ruleID }
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to delete groupFund rule"
                    handleFetchError(errorMsg) {
                        deleteRule(rule, showLoader, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun updateRule(rule: GroupFundRule, completion: (Boolean, String?) -> Unit) {
        val currentGroupFundID = _groupFund.value?.groupFundID
        if (currentGroupFundID == null) {
            completion(false, "Group Fund ID not found.")
            return
        }

        manager.updateGroupFundRule(currentGroupFundID, rule) { success, error ->
            MainScope().launch {
                if (success) {
                    val index = _rules.value.indexOfFirst { it.id == rule.id }
                    if (index != -1) _rules.value = _rules.value.toMutableList().apply { set(index, rule) }
                    completion(true, null)
                } else {
                    completion(false, error ?: "Failed to update rule.")
                }
            }
        }
    }

    fun updateContributionStatus(
        showLoader: Boolean = true,
        groupFundID: String,
        memberID: String,
        contributionID: String,
        newStatus: String,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateContributionStatus(groupFundID, memberID, contributionID, newStatus) { success, message ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (!success) {
                val errorMsg = message ?: "❌ Failed to update contribution status"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    updateContributionStatus(showLoader, groupFundID, memberID, contributionID, newStatus, completion)
                }
                completion(false, errorMsg)
                return@updateContributionStatus
            }

            val index = _selectedContributions.value.indexOfFirst { it.id == contributionID && it.memberID == memberID }
            if (index != -1) {
                _selectedContributions.value = _selectedContributions.value.toMutableList().apply {
                    this[index] = this[index].copy(
                        paidStatus = PaidStatus.PAID,
                        paidOn = Timestamp(Date())
                    )
                }
            }

            completion(true, message)
        }
    }

    fun updateLoanStatus(
        groupFundID: String,
        memberID: String,
        loanID: String,
        status: String,
        showLoader: Boolean = true,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            completion(false, SquadStrings.networkError)
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateLoanStatus(groupFundID, memberID, loanID, status) { success, message ->
            if (showLoader) LoaderManager.shared.hideLoader()
            completion(success, message)
        }
    }

    fun updateInstallmentStatus(
        groupFundID: String,
        memberID: String,
        loanID: String,
        installmentID: String,
        status: String,
        showLoader: Boolean = true,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            completion(false, SquadStrings.networkError)
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateInstallmentStatus(
            groupFundID = groupFundID,
            memberID = memberID,
            loanID = loanID,
            installmentID = installmentID,
            status = status
        ) { success, message ->
            if (showLoader) LoaderManager.shared.hideLoader()
            completion(success, message)
        }
    }

    fun updatePaymentStatus(
        showLoader: Boolean = true,
        groupFundID: String,
        paymentID: String,
        status: String,
        reason: String,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updatePaymentStatus(
            groupFundID = groupFundID,
            paymentID = paymentID,
            status = status,
            reason = reason
        ) { success, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (!success) {
                val errorMsg = error ?: "❌ Failed to update payment status"
                println("SavingsSquadPayment ❌ Failed to update payment status")
                println(errorMsg)
                handleFetchError(errorMsg) {
                    updatePaymentStatus(showLoader, groupFundID, paymentID, status, reason, completion)
                }
                completion(false, errorMsg)
                return@updatePaymentStatus
            }

            val index = _groupFundPayments.value.indexOfFirst { it.id == paymentID }
            if (index != -1) {
                println("SavingsSquadPayment local cache updated")

                _groupFundPayments.value = _groupFundPayments.value.toMutableList().apply {
                    val updatedPayment = this[index].copy(
                        paymentStatus = if (status == "SUCCESS") PaymentStatus.SUCCESS else PaymentStatus.FAILED,
                        paymentUpdatedDate = Timestamp(Date())
                    )
                    this[index] = updatedPayment
                }
            }

            completion(true, null)
        }
    }

    fun savePayments(
        showLoader: Boolean = true,
        groupFundID: String,
        payment: List<PaymentsDetails>,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.savePayments(groupFundID, payment) { success, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (!success) {
                val errorMsg = error ?: "❌ Failed to add payment"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    savePayments(showLoader, groupFundID, payment, completion)
                }
                completion(false, errorMsg)
                return@savePayments
            }

            _groupFundPayments.value = _groupFundPayments.value.toMutableList().apply {
                addAll(payment)
            }

            val groupFundLocal = _groupFund.value
            if (groupFundLocal == null) {
                completion(false, "❌ GroupFund not found")
                return@savePayments
            }

            val userId = payment.firstOrNull()?.memberId
            val member = _groupFundMembers.value.firstOrNull { it.id == userId }
            if (member == null) {
                completion(false, "❌ Member not found")
                return@savePayments
            }

            var groupFundCopy = groupFundLocal
            var memberCopy = member
            applyPaymentSummaries(payment, groupFundCopy, memberCopy)

            CoroutineScope(Dispatchers.IO).launch {
                updateMembers(groupFundID = groupFundCopy.groupFundID, members = listOf(memberCopy)) { memberSuccess, memberError ->
                    if (!memberSuccess) {
                        println("❌ Failed to update member: ${memberError ?: "Unknown error"}")
                    }
                }

                updateGroupFund(groupFund = groupFundCopy) { groupFundSuccess, _, groupFundError ->
                    if (!groupFundSuccess) {
                        println("❌ Failed to update groupFund: ${groupFundError ?: "Unknown error"}")
                    }
                }
            }

            completion(true, null)
        }
    }

    fun applyPaymentSummaries(payments: List<PaymentsDetails>, groupFund: GroupFund, member: Member) {
        for (pay in payments) {
            when (pay.paymentType) {
                PaymentType.PAYMENT_CREDIT -> {
                    when (pay.paymentSubType) {
                        PaymentSubType.INTEREST_AMOUNT -> {
                            groupFund.totalInterestAmountReceived += pay.intrestAmount
                            member.totalInterestPaid += pay.intrestAmount
                        }

                        PaymentSubType.EMI_AMOUNT -> {
                            groupFund.totalLoanAmountReceived += (pay.amount - pay.intrestAmount)
                            member.totalLoanPaid += (pay.amount - pay.intrestAmount)
                        }

                        PaymentSubType.CONTRIBUTION_AMOUNT -> {
                            groupFund.totalContributionAmountReceived += pay.amount
                            member.totalContributionPaid += pay.amount
                        }

                        PaymentSubType.OTHERS_AMOUNT -> Unit
                        else -> Unit
                    }
                    groupFund.currentAvailableAmount += pay.amount
                }

                PaymentType.PAYMENT_DEBIT -> {
                    if (pay.paymentSubType == PaymentSubType.LOAN_AMOUNT) {
                        groupFund.totalLoanAmountSent += (pay.amount - pay.intrestAmount)
                        member.totalLoanBorrowed += (pay.amount - pay.intrestAmount)
                        groupFund.currentAvailableAmount -= pay.amount
                    }
                }
            }
        }
    }

    fun updateMembers(
        showLoader: Boolean = true,
        groupFundID: String,
        members: List<Member>,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateMembers(groupFundID, members) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    println("✅ Members updated successfully!")
                    for (updatedMember in members) {
                        val currentList = _groupFundMembers.value.toMutableList()
                        val index = currentList.indexOfFirst { it.id == updatedMember.id }
                        if (index != -1) {
                            currentList[index] = updatedMember
                            _groupFundMembers.value = currentList
                        }
                    }

                    _groupFundMembersCount.value = _groupFundMembers.value.size
                    _groupFundMemberNames.value = _groupFundMembers.value.map { it.name }

                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to update members"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        updateMembers(showLoader, groupFundID, members, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun updateMemberMobileNumber(
        showLoader: Boolean,
        groupFundID: String,
        memberID: String,
        mobileNumber: String,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateMemberMobileNumber(groupFundID, memberID, mobileNumber) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to updateMemberMobileNumber"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        updateMemberMobileNumber(showLoader, groupFundID, memberID, mobileNumber, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun fetchPayments(showLoader: Boolean, completion: (Boolean, String?) -> Unit) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.fetchPayments(groupFundID) { fetchedPayments, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (fetchedPayments != null) {
                    _groupFundPayments.value = fetchedPayments.toMutableList()
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to fetch payments"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        fetchPayments(showLoader, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun observePayments() {
        manager.observePayments(groupFundID) { updatedPayments, error ->
            if (updatedPayments != null) {
                _groupFundPayments.value = updatedPayments
            } else {
                val errorMsg = error ?: "❌ Failed to observe payments"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    observePayments()
                }
            }
        }
    }

    fun addEMIConfiguration(
        showLoader: Boolean,
        emi: EMIConfiguration,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.addEMIConfiguration(groupFundID, emi) { success, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (success) {
                completion(true, null)
            } else {
                val errorMsg = error ?: "❌ Failed to append EMI configuration"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    addEMIConfiguration(showLoader, emi, completion)
                }
                completion(false, errorMsg)
            }
        }
    }

    fun addOrUpdateEMIConfiguration(
        showLoader: Boolean,
        groupFundID: String,
        emi: EMIConfiguration,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.addOrUpdateEMIConfiguration(groupFundID, emi) { success, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (success) {
                println("✅ EMI Configuration added/updated successfully!")

                _emiConfigurations.value = _emiConfigurations.value.toMutableList().apply {
                    val index = indexOfFirst { it.id == emi.id }
                    if (index != -1) {
                        this[index] = emi // Update existing item
                    } else {
                        add(emi) // Add new item
                    }
                }

                completion(true, null)
            } else {
                val errorMsg = error ?: "❌ Failed to add/update EMI configuration"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    addOrUpdateEMIConfiguration(showLoader, groupFundID, emi, completion)
                }
                completion(false, errorMsg)
            }
        }
    }

    fun fetchEMIConfigurations(showLoader: Boolean, completion: (Boolean, String?) -> Unit) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.fetchEMIConfigurations(groupFundID) { emiList, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (emiList != null) {
                _emiConfigurations.value= emiList.toMutableList()
                completion(true, null)
            } else {
                val errorMsg = error ?: "❌ Failed to fetch EMI configurations"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    fetchEMIConfigurations(showLoader, completion)
                }
                completion(false, errorMsg)
            }
        }
    }

    fun deleteEMIConfiguration(showLoader: Boolean, emiID: String, completion: (Boolean, String?) -> Unit) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.deleteEMIConfiguration(groupFundID, emiID) { success, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (success) {
                _emiConfigurations.value = _emiConfigurations.value.filter { it.id != emiID }
                completion(true, null)
            } else {
                val errorMsg = error ?: "❌ Failed to delete EMI configuration"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    deleteEMIConfiguration(showLoader, emiID, completion)
                }
                completion(false, errorMsg)
            }
        }
    }

    fun fetchMemberLoans(showLoader: Boolean, memberID: String, completion: (Boolean, String?) -> Unit) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        _isPendingLoanAvailable.value = false

        manager.fetchMemberLoans(groupFundID, memberID) { loans, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (loans != null) {
                _memberLoans.value = loans
                _memberPendingLoans.value = loans.pendingLoans()
                _isPendingLoanAvailable.value = !(memberPendingLoans.value?.isEmpty() ?: true)

                if (_isPendingLoanAvailable.value) {
                    updateLoanPaidAfterInstallmentSettled(_memberPendingLoans.value ?: emptyList(), memberID)
                }

                completion(true, null)
            } else {
                val errorMsg = error ?: "Failed to fetch EMIs"
                handleFetchError(errorMsg) {
                    fetchMemberLoans(showLoader, memberID, completion)
                }
                completion(false, errorMsg)
            }
        }
    }

    private fun updateLoanPaidAfterInstallmentSettled(loans: List<MemberLoan>, memberID: String) {
        if (loans.isNotEmpty()) {
            viewModelScope.launch {
                memberPendingLoans.collect { loans ->
                    loans?.forEach { loan ->
                        // If all installments are paid
                        if (loan.installments.pendingInstallments().isEmpty()) {

                            // Only update if still pending
                            if (loan.loanStatus == EMIStatus.PENDING) {
                                val updatedLoan = loan.copy(
                                    loanStatus = EMIStatus.PAID,
                                    loanClosedDate = Date().asTimestamp
                                )

                                addOrUpdateMemberLoan(
                                    showLoader = true,
                                    memberID = memberID,
                                    loan = updatedLoan
                                ) { success, error ->
                                    if (success) {
                                        Log.d("Loans", "✅ Loan ${loan.id} marked as PAID")
                                    } else {
                                        Log.e("Loans", "❌ Failed to update loan: $error")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun addOrUpdateMemberLoan(
        showLoader: Boolean,
        memberID: String,
        loan: MemberLoan,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }
        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.addOrUpdateMemberLoan(groupFundID, memberID, loan) { success, error ->
            Handler(Looper.getMainLooper()).post {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    println("✅ Member loan added/updated successfully!")
                    fetchMemberLoans(false, memberID) { _, _ -> }
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to add/update member loan"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        addOrUpdateMemberLoan(showLoader, memberID, loan, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun fetchAllLoansInGroupFund(
        showLoader: Boolean = true,
        groupFundID: String,
        completion: (Boolean, List<MemberLoan>?, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }
        if (showLoader) LoaderManager.shared.showLoader()

        val db = FirebaseFirestore.getInstance()
        val membersRef = db.collection("groupFunds").document(groupFundID).collection("members")
        val allLoans = mutableListOf<MemberLoan>()
        val errors = mutableListOf<String>()

        membersRef.get().addOnCompleteListener { memberTask ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (!memberTask.isSuccessful) {
                completion(false, null, "❌ Failed to fetch members: ${memberTask.exception?.localizedMessage}")
                return@addOnCompleteListener
            }

            val members = memberTask.result?.documents ?: emptyList()
            if (members.isEmpty()) {
                completion(false, null, "❌ No members found")
                return@addOnCompleteListener
            }

            if (showLoader) LoaderManager.shared.showLoader()

            val latch = CountDownLatch(members.size)
            for (memberDoc in members) {
                val memberID = memberDoc.id
                val loansRef = membersRef.document(memberID).collection("loans")

                loansRef.get().addOnCompleteListener { loanTask ->
                    if (!loanTask.isSuccessful) {
                        errors.add("❌ Error fetching loans for $memberID: ${loanTask.exception?.localizedMessage}")
                    } else {
                        for (loanDoc in loanTask.result!!) {
                            val loan = loanDoc.toObject(MemberLoan::class.java)
                            allLoans.add(loan)
                        }
                    }
                    latch.countDown()
                }
            }

            Thread {
                latch.await()
                Handler(Looper.getMainLooper()).post {
                    if (showLoader) LoaderManager.shared.hideLoader()
                    if (errors.isNotEmpty()) {
                        completion(false, allLoans, errors.joinToString("\n"))
                    } else {
                        completion(true, allLoans, null)
                    }
                }
            }.start()
        }
    }

    fun deleteMemberLoan(
        showLoader: Boolean,
        memberID: String,
        loanID: String,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }
        if (showLoader) LoaderManager.shared.showLoader()

        manager.deleteMemberLoan(groupFundID, memberID, loanID) { success, error ->
            Handler(Looper.getMainLooper()).post {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    _memberLoans.value = _memberLoans.value.filter { it.id != loanID }
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to delete loan"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        deleteMemberLoan(showLoader, memberID, loanID, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun addOrUpdateInstallment(
        showLoader: Boolean,
        memberID: String,
        loanID: String,
        installment: Installment,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }
        if (showLoader) LoaderManager.shared.showLoader()

        manager.addOrUpdateInstallment(groupFundID, memberID, loanID, installment) { success, error ->
            Handler(Looper.getMainLooper()).post {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    fetchMemberLoans(false, memberID) { _, _ -> }
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to add/update installment"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        addOrUpdateInstallment(showLoader, memberID, loanID, installment, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun removeInstallment(
        showLoader: Boolean,
        memberID: String,
        loanID: String,
        installment: Installment,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }
        if (showLoader) LoaderManager.shared.showLoader()

        manager.removeInstallment(groupFundID, memberID, loanID, installment) { success, error ->
            Handler(Looper.getMainLooper()).post {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    fetchMemberLoans(false, memberID) { _, _ -> }
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to remove installment"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        removeInstallment(showLoader, memberID, loanID, installment, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    private fun handleFetchError(error: String, retryAction: () -> Unit) {
        AlertManager.shared.showAlert(
            title = SquadStrings.appName,
            message = error,
            primaryButtonTitle = "OK",
            primaryAction = {},
            secondaryButtonTitle = "Retry",
            secondaryAction = retryAction
        )
    }

    fun createGroupFundActivity(
        activityType: GroupFundActivityType,
        userName: String,
        amount: Int,
        description: String,
        alertOK: (() -> Unit)? = null
    ) {
        val groupFund = _groupFund.value ?: run {
            println("❌ No groupFund found!")
            return
        }

        val activity = GroupFundActivity(
            groupFundID = groupFund.groupFundID,
            groupFundName = groupFund.groupFundName,
            date = Timestamp.now(),
            activityType = activityType,
            userName = userName,
            amount = amount,
            description = description
        )

        if (UserDefaultsManager.getLogin()?.groupFundID != null) {
            addGroupFundActivity(true, activity) { success, error ->
                if (success) {
                    println("✅ Activity added successfully!")
                    AlertManager.shared.showAlert(
                        title = SquadStrings.appName,
                        message = description,
                        primaryButtonTitle = "OK",
                        primaryAction = alertOK ?: {}
                    )
                } else {
                    handleFetchError(error ?: "Unknown error") {
                        createGroupFundActivity(activityType, userName, amount, description, alertOK)
                    }
                }
            }
        } else {
            println("❌ No groupFund found!")
        }
    }

    fun fetchDueContributionsAndInstallments(
        groupFundID: String,
        completion: (List<ContributionDetail>, List<Installment>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val membersRef = db.collection("groupFunds").document(groupFundID).collection("members")

        val dueContributions = mutableListOf<ContributionDetail>()
        val dueInstallments = mutableListOf<Installment>()

        membersRef.get().addOnSuccessListener { snapshot ->
            val members = snapshot.documents
            val latch = CountDownLatch(members.size)

            for (memberDoc in members) {
                val memberID = memberDoc.id
                val contribRef = membersRef.document(memberID).collection("contributions")
                val loansRef = membersRef.document(memberID).collection("loans")

                contribRef.get().addOnSuccessListener { contribSnap ->
                    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    for (doc in contribSnap.documents) {
                        val contribution = doc.toObject(ContributionDetail::class.java)
                        contribution?.let {
                            val dueDate = formatter.parse(it.monthYear)
                            if (it.paidStatus == PaidStatus.NOT_PAID && dueDate != null && dueDate.before(Date())) {
                                dueContributions.add(it)
                            }
                        }
                    }
                    latch.countDown()
                }

                loansRef.get().addOnSuccessListener { loanSnap ->
                    for (loanDoc in loanSnap.documents) {
                        val loan = loanDoc.toObject(MemberLoan::class.java)
                        loan?.let {
                            val calendar = Calendar.getInstance()
                            val current = calendar.get(Calendar.MONTH) + calendar.get(Calendar.YEAR) * 12

                            for (inst in it.installments) {
                                val due = inst.dueDate?.toDate()
                                if (inst.status == EMIStatus.PENDING && due != null) {
                                    val dueCal = Calendar.getInstance().apply { time = due }
                                    val dueKey = dueCal.get(Calendar.MONTH) + dueCal.get(Calendar.YEAR) * 12
                                    if (dueKey <= current) {
                                        dueInstallments.add(inst)
                                    }
                                }
                            }
                        }
                    }
                    latch.countDown()
                }
            }

            Thread {
                latch.await()
                Handler(Looper.getMainLooper()).post {
                    completion(dueContributions, dueInstallments)
                }
            }.start()
        }.addOnFailureListener {
            println("❌ Error fetching members: ${it.localizedMessage}")
            completion(emptyList(), emptyList())
        }
    }

    fun handleCashFreeResponse(sessionId: String?, orderId: String?, error: Exception?) {
        if (error != null) {
            val nsError = error
            val errorMessage: String

            val serverMessage = nsError.message
            val localizedMessage = nsError.localizedMessage
            val userInfoMessage = if (nsError is java.net.SocketTimeoutException) "Request timed out" else null

            errorMessage = when {
                !serverMessage.isNullOrBlank() -> serverMessage
                !localizedMessage.isNullOrBlank() -> localizedMessage
                !userInfoMessage.isNullOrBlank() -> userInfoMessage
                else -> "Something went wrong while creating the payment entry. Please try again."
            }

            Log.d("Payment", "❌ Payment error [Code: 0] - $errorMessage")
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = errorMessage,
                type = AlertType.ERROR,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (sessionId.isNullOrEmpty() || orderId.isNullOrEmpty()) {
            Log.d("Payment", "❌ Missing or empty sessionId/orderId in response")
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "Payment entry could not be created. Please try again.",
                type = AlertType.ERROR,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        Log.d("Payment", "✅ Payment entry ready | orderId: $orderId, sessionId: $sessionId")
        _showPayment.value = true
        _paymentOrderToken.value = sessionId
        _paymentOrderId.value = orderId
    }

    //Payment Changes
    fun retryPayoutAction(payment: PaymentsDetails) {
        viewModelScope.launch {
            if (payment.payoutStatus == PayoutStatus.PENDING || payment.payoutStatus == PayoutStatus.FAILED) {

                LoaderManager.shared.showLoader()
                val isManager = UserDefaultsManager.getGroupFundManagerLogged()

                val beneId = if (isManager)
                    groupFund.value?.upiBeneId ?: ""
                else
                    currentMember.value?.upiBeneId ?: ""

                FirebaseFunctionsManager.shared.makeCashFreePayout(
                    groupFundId = groupFund.value?.groupFundID ?: "",
                    paymentId = payment.id ?: "",
                    beneId = beneId,
                    amount = payment.amount.toDouble(),
                    transferType = "UPI",
                    description = payment.description,
                    memberId = payment.memberId,
                    memberName = payment.memberName,
                    userType = payment.userType.value,
                    paymentEntryType = payment.paymentEntryType.value,
                    paymentType = payment.paymentType.value,
                    paymentSubType = payment.paymentSubType.value,
                    contributionId = payment.contributionId,
                    loanId = payment.loanId,
                    installmentId = payment.installmentId,
                    transferMode = "IMPS"
                ) { result ->
                    LoaderManager.shared.hideLoader()

                    result.onSuccess { data ->
                        val status = data.status?.uppercase()
                        val updatedOnStr = data.updatedOn
                        val transferId = data.transferId

                        val updatedTimestamp = CommonFunctions.parseISODateToTimestamp(updatedOnStr)

                        // Update payment model in local list
                        val index = _groupFundPayments.value.indexOfFirst { it.id == payment.id }
                        if (index != -1) {
                            val updatedPayment = _groupFundPayments.value[index].copy(
                                payoutStatus = when (status) {
                                    "SUCCESS" -> PayoutStatus.SUCCESS
                                    "FAILED" -> PayoutStatus.FAILED
                                    "RECEIVED", "PENDING", "IN_PROGRESS" -> PayoutStatus.IN_PROGRESS
                                    else -> PayoutStatus.PENDING
                                },
                                payoutSuccess = status == "SUCCESS",
                                transferReferenceId = transferId ?: "",
                                paymentUpdatedDate = updatedTimestamp,
                                payoutResponseMessage = data.statusDescription ?: ""
                            )

                            val updatedList = _groupFundPayments.value.toMutableList()
                            updatedList[index] = updatedPayment
                            _groupFundPayments.value = updatedList

                            AlertManager.shared.showAlert(
                                title = SquadStrings.appName,
                                message = updatedPayment.payoutResponseMessage,
                                type = if (updatedPayment.payoutStatus == PayoutStatus.FAILED)
                                    AlertType.ERROR else AlertType.SUCCESS,
                                primaryButtonTitle = "OK"
                            )
                        }
                    }

                    result.onFailure { error ->
                        val message = error.message ?: "Failed to process payout. Please try again."
                        AlertManager.shared.showAlert(
                            title = SquadStrings.appName,
                            message = message,
                            type = AlertType.ERROR,
                            primaryButtonTitle = "OK"
                        )
                    }
                }

            } else {
                // ✅ Verify existing payout instead of retrying
                LoaderManager.shared.showLoader()
                FirebaseFunctionsManager.shared.verifyCashFreePayoutStatus(
                    groupFundId = groupFund.value?.groupFundID ?: "",
                    paymentId = payment.id ?: "",
                    transferId = payment.transferReferenceId
                ) { result ->
                    LoaderManager.shared.hideLoader()

                    result.onSuccess { data ->
                        val status = data.status?.uppercase()

                        val updatedPayment = payment.copy(
                            payoutStatus = when (status) {
                                "SUCCESS" -> PayoutStatus.SUCCESS
                                "FAILED" -> PayoutStatus.FAILED
                                "PENDING", "IN_PROGRESS" -> PayoutStatus.IN_PROGRESS
                                else -> PayoutStatus.PENDING
                            },
                            payoutSuccess = status == "SUCCESS",
                            payoutResponseMessage = data.statusDescription ?: "",
                            paymentUpdatedDate = CommonFunctions.parseISODateToTimestamp(data.updatedOn)
                        )

                        val updatedList = _groupFundPayments.value.toMutableList()
                        val index = updatedList.indexOfFirst { it.id == payment.id }
                        if (index != -1) {
                            updatedList[index] = updatedPayment
                            _groupFundPayments.value = updatedList
                        }

                        if (updatedPayment.payoutStatus == PayoutStatus.SUCCESS) {
                            AlertManager.shared.showAlert(
                                title = SquadStrings.appName,
                                message = updatedPayment.payoutResponseMessage,
                                type = AlertType.SUCCESS,
                                primaryButtonTitle = "OK"
                            )
                        }
                    }

                    result.onFailure { error ->
                        val message = error.message ?: "Failed to verify payout status. Please try again."
                        AlertManager.shared.showAlert(
                            title = SquadStrings.appName,
                            message = message,
                            type = AlertType.ERROR,
                            primaryButtonTitle = "OK"
                        )
                    }
                }
            }
        }
    }

    fun retryPaymentAction(payment: PaymentsDetails) {
        viewModelScope.launch {
            if (payment.paymentStatus == PaymentStatus.PENDING || payment.paymentStatus == PaymentStatus.FAILED) {
                LoaderManager.shared.showLoader()

                FirebaseFunctionsManager.shared.processCashFreePayment(
                    groupFundId = payment.groupFundId,
                    action = CashfreePaymentAction.Retry(failedOrderId = payment.orderId)
                ) { sessionId, orderId, error ->
                    LoaderManager.shared.hideLoader()
                    handleCashFreeResponse(sessionId, orderId, error)
                }
            }
        }
    }
}