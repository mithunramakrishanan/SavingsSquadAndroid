package com.android.savingssquad.viewmodel

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.android.savingssquad.model.CashRequest
import com.android.savingssquad.model.CashRequestStatus
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Login
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.model.SquadActivity
import com.android.savingssquad.model.EMIConfiguration
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.SquadRule
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.ReminderRequest
import com.android.savingssquad.singleton.PayoutStatus
import com.android.savingssquad.model.pendingInstallments
import com.android.savingssquad.model.pendingLoans
import com.android.savingssquad.singleton.AlertType
import com.android.savingssquad.singleton.AmountEditType
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.NotificationService
import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentApproveStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentFilter
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.orNow
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.RazorpayPaymentAction
import com.android.savingssquad.singleton.SessionManager
import com.android.savingssquad.singleton.UPIPaymentManager
import com.android.savingssquad.singleton.UPIPaymentStatus
import com.google.firebase.firestore.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.yourapp.utils.CommonFunctions
import com.yourapp.utils.IDGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Date
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.CountDownLatch

class SquadViewModel : ViewModel() {

    // ------------------------------------------------------------------------
    // 🔹 Firestore + Initialization
    // ------------------------------------------------------------------------
    private val manager = FirestoreManager.shared
    private var squadID: String = ""
    var loginMember: Login? = null

    // ------------------------------------------------------------------------
    // 🔹 Squad
    // ------------------------------------------------------------------------
    private val _squad = MutableStateFlow<Squad?>(null)
    val squad: StateFlow<Squad?> = _squad
    fun setSquad(value: Squad?) { _squad.value = value }

    private val _pendingApprovalPayments = MutableStateFlow<List<PaymentsDetails>>(emptyList())
    val pendingApprovalPayments: StateFlow<List<PaymentsDetails>> = _pendingApprovalPayments
    fun setPendingApprovalPayments(list: List<PaymentsDetails>) { _pendingApprovalPayments.value = list }

    private val _rules = MutableStateFlow<List<SquadRule>>(emptyList())
    val rules: StateFlow<List<SquadRule>> = _rules
    fun setRules(list: List<SquadRule>) { _rules.value = list }

    private val _squadActivities = MutableStateFlow<List<SquadActivity>>(emptyList())
    val squadActivities: StateFlow<List<SquadActivity>> = _squadActivities
    fun setSquadActivities(list: List<SquadActivity>) { _squadActivities.value = list }

    var isLoadingMoreActivities by mutableStateOf(false)

        private set

    private var activitiesLastDocument: DocumentSnapshot? = null

    private var activitiesIsLoadingMore = false

    private var activitiesHasMoreData = true

    private val activitiesPageSize = 20

    private val _squadPayments = MutableStateFlow<List<PaymentsDetails>>(emptyList())
    val squadPayments: StateFlow<List<PaymentsDetails>> = _squadPayments
    fun setSquadPayments(list: List<PaymentsDetails>) { _squadPayments.value = list }

    private var paymentsLastDocument: DocumentSnapshot? = null

    var paymentsIsLoadingMore: Boolean = false

    private var paymentsHasMoreData: Boolean = true

    private val paymentsPageSize: Int = 20


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

    private val _squadMembers = MutableStateFlow<List<Member>>(emptyList())
    val squadMembers: StateFlow<List<Member>> = _squadMembers
    fun setSquadMembers(list: List<Member>) {
        _squadMembers.value = list
        _squadMembersCount.value = list.size
    }

    private val _squadMembersCount = MutableStateFlow(0)
    val squadMembersCount: StateFlow<Int> = _squadMembersCount
    fun setSquadMembersCount(value: Int) { _squadMembersCount.value = value }

    private val _memberDetail = MutableStateFlow<Member?>(null)
    val memberDetail: StateFlow<Member?> = _memberDetail
    fun setMemberDetail(value: Member?) { _memberDetail.value = value }

    private val _currentMember = MutableStateFlow<Member?>(null)
    val currentMember: StateFlow<Member?> = _currentMember
    fun setCurrentMember(value: Member?) { _currentMember.value = value }

    private val _selectedMember = MutableStateFlow<Member?>(null)
    val selectedMember: StateFlow<Member?> = _selectedMember
    fun setSelectedMember(value: Member?) { _selectedMember.value = value }

    private val _selectedContributions = MutableStateFlow<List<ContributionDetail>>(emptyList())
    val selectedContributions: StateFlow<List<ContributionDetail>> = _selectedContributions
    fun setSelectedContributions(list: List<ContributionDetail>) { _selectedContributions.value = list }

    private val _squadMemberNames = MutableStateFlow<List<String>>(emptyList())
    val squadMemberNames: StateFlow<List<String>> = _squadMemberNames
    fun setSquadMemberNames(list: List<String>) { _squadMemberNames.value = list }

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

    // ------------------------------------------------------------------------
    // 🔹 UI States / Popups
    // ------------------------------------------------------------------------
    private val _showPopup = MutableStateFlow(false)
    val showPopup: StateFlow<Boolean> = _showPopup
    fun setShowPopup(value: Boolean) { _showPopup.value = value }


    private val _showRequestCashPopup = MutableStateFlow(false)
    val showRequestCashPopup: StateFlow<Boolean> = _showRequestCashPopup
    fun setShowRequestCashPopup(value: Boolean) { _showRequestCashPopup.value = value }

    private val _showAddMemberPopup = MutableStateFlow(false)
    val showAddMemberPopup: StateFlow<Boolean> = _showAddMemberPopup
    fun setShowAddMemberPopup(value: Boolean) { _showAddMemberPopup.value = value }

    private val _showUpdateMemberPopup = MutableStateFlow(false)
    val showUpdateMemberPopup: StateFlow<Boolean> = _showUpdateMemberPopup
    fun setShowUpdateMemberPopup(value: Boolean) { _showUpdateMemberPopup.value = value }


    private val _showEditAmountPopup = MutableStateFlow(false)
    val showEditAmountPopup: StateFlow<Boolean> = _showEditAmountPopup
    fun setShowEditAmountPopup(value: Boolean) { _showEditAmountPopup.value = value }


    private val _showContributionMemberPopup = MutableStateFlow(false)
    val showContributionMemberPopup: StateFlow<Boolean> = _showContributionMemberPopup
    fun setShowContributionMemberPopup(value: Boolean) { _showContributionMemberPopup.value = value }


    private val _showEMIMemberPopup = MutableStateFlow(false)
    val showEMIMemberPopup: StateFlow<Boolean> = _showEMIMemberPopup
    fun setShowEMIMemberPopup(value: Boolean) { _showEMIMemberPopup.value = value }

    private val _showContributionMonthPopup = MutableStateFlow(false)
    val showContributionMonthPopup: StateFlow<Boolean> = _showContributionMonthPopup
    fun setShowContributionMonthPopup(value: Boolean) { _showContributionMonthPopup.value = value }


    private val _showEMIMonthPopup = MutableStateFlow(false)
    val showEMIMonthPopup: StateFlow<Boolean> = _showEMIMonthPopup
    fun setShowEMIMonthPopup(value: Boolean) { _showEMIMonthPopup.value = value }


    private val _showLoanMembersPopup = MutableStateFlow(false)
    val showLoanMembersPopup: StateFlow<Boolean> = _showLoanMembersPopup
    fun setShowLoanMembersPopupPopup(value: Boolean) { _showLoanMembersPopup.value = value }

    private val _showEMIConfigPopup = MutableStateFlow(false)
    val showEMIConfigPopup: StateFlow<Boolean> = _showEMIConfigPopup
    fun setShowEMIConfigPopup(value: Boolean) { _showEMIConfigPopup.value = value }

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

    private val _editAmountType = MutableStateFlow<AmountEditType?>(null)
    val editAmountType: StateFlow<AmountEditType?> = _editAmountType
    fun setEditAmountType(value: AmountEditType?) { _editAmountType.value = value }


    private val _showUpgradePlan = MutableStateFlow(false)
    val showUpgradePlan: StateFlow<Boolean> = _showUpgradePlan
    fun setShowUpgradePlan(value: Boolean) { _showUpgradePlan.value = value }

    private val _showUpgradeSuccess = MutableStateFlow(false)
    val showUpgradeSuccess: StateFlow<Boolean> = _showUpgradeSuccess
    fun setShowUpgradeSuccess(value: Boolean) { _showUpgradeSuccess.value = value }


    private val _managerLogins = MutableStateFlow<List<Login>>(emptyList())
    val managerLogins: StateFlow<List<Login>> = _managerLogins
    fun setManagerLogins(list: List<Login>) { _managerLogins.value = list }


    // Pagination State
    var cashRequestsLastDocument: DocumentSnapshot? = null
    private val cashRequestsPageSize : Int = 20

    var cashRequestsHasMoreData = true
    var cashRequestsIsLoadingMore = false

    private val _squadCashRequests = mutableStateOf<List<CashRequest>>(emptyList())
    val squadCashRequests: State<List<CashRequest>> = _squadCashRequests

    fun setSquadCashRequests(cashRequests: List<CashRequest>) {
        _squadCashRequests.value = cashRequests
    }

    private val _verifySquadManagerAmountBadgeCount = MutableStateFlow(0)
    val verifySquadManagerAmountBadgeCount: StateFlow<Int> = _verifySquadManagerAmountBadgeCount
    fun setVerifySquadManagerAmountBadgeCount(count: Int) { _verifySquadManagerAmountBadgeCount.value = count }

    private val _verifySquadCashRequestBadgeCount = MutableStateFlow(0)
    val verifySquadCashRequestBadgeCount: StateFlow<Int> = _verifySquadCashRequestBadgeCount
    fun setVerifySquadCashRequestBadgeCount(count: Int) { _verifySquadCashRequestBadgeCount.value = count }

    private val _verifySquadMemberAmountBadgeCount = MutableStateFlow(0)
    val verifySquadMemberAmountBadgeCount: StateFlow<Int> = _verifySquadMemberAmountBadgeCount
    fun setVerifySquadMemberAmountBadgeCount(count: Int) { _verifySquadMemberAmountBadgeCount.value = count }

    private var squadListener: ListenerRegistration? = null
    private var membersListener: ListenerRegistration? = null



    init {
        val login = UserDefaultsManager.getLogin()
        if (login != null) {
            squadID = login.squadID
            loginMember = login
            setSelectedUser(login)

            startObservers(login.squadID)

        } else {
            val defaultLogin = Login(
                squadID = "",
                squadName = "",
                squadUsername = "",
                squadUserId = "",
                phoneNumber = "",
                role = SquadUserType.SQUAD_MANAGER,
                squadCreatedDate = Date().asTimestamp,
                userCreatedDate = Date().asTimestamp
            )
            squadID = ""
            loginMember = defaultLogin

            setSelectedUser(defaultLogin)
        }
    }


    fun startObservers(squadID: String) {

        this.squadID = squadID

        // Stop previous listeners
        stopObservers()

        // Start new listeners
        observeSquadChanges()
        observeMembers()
    }

    fun stopObservers() {

        squadListener?.remove()
        squadListener = null

        membersListener?.remove()
        membersListener = null
    }


    fun observeSquadChanges() {

        if (squadID.isEmpty()) {
            Log.w("Firestore", "observeSquadChanges: Squad ID is empty")
            return
        }

        squadListener?.remove()

        squadListener = FirebaseFirestore.getInstance()
            .collection("squads")
            .document(squadID)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e(
                        "Firestore",
                        "Failed to observe squad",
                        error
                    )
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w("Firestore", "Squad snapshot is null")
                    return@addSnapshotListener
                }

                if (!snapshot.exists()) {
                    Log.w("Firestore", "Squad document does not exist")
                    return@addSnapshotListener
                }

                try {

                    val squad = snapshot.toObject(Squad::class.java)

                    if (squad != null) {

                        setSquad(squad)
                        setVerifySquadManagerAmountBadgeCount(squad.verifyAmountCount)
                        setVerifySquadCashRequestBadgeCount(squad.cashRequestedCount)
                        setRemainingMonths(CommonFunctions.getRemainingMonths(
                            Date(),
                            squad.squadEndDate?.toDate() ?: Date()
                        ))
                    }

                } catch (e: Exception) {

                    Log.e(
                        "Firestore",
                        "Failed to decode Squad",
                        e
                    )
                }
            }
    }

    fun observeMembers() {

        membersListener?.remove()

        membersListener = FirebaseFirestore.getInstance()
            .collection("squads")
            .document(squadID)
            .collection("members")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e(
                        "Firestore",
                        "Members Listener",
                        error
                    )
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                try {

                    val fetchedMembers = snapshot.documents.mapNotNull {
                        it.toObject(Member::class.java)
                    }

                    setSquadMembers(fetchedMembers)
                    setSquadMembersCount(fetchedMembers.size)
                    setSquadMemberNames(fetchedMembers.map { it.name })

                    currentMember.let { member ->
                        setCurrentMember( fetchedMembers.find { it.id == member.value?.id })
                        setVerifySquadMemberAmountBadgeCount(member.value?.verifyAmountCount ?: 0)
                    }

                    loginMember?.squadUsername?.let { username ->

                        val member = CommonFunctions.getMemberByName(
                            username,
                            fetchedMembers
                        )

                        if (member != null) {
                            setMemberDetail(member)
                        }
                    }

                } catch (e: Exception) {

                    Log.e(
                        "Firestore",
                        "Failed to decode members",
                        e
                    )
                }
            }
    }

    fun addUserLogin(showLoader: Boolean = true, member: Member) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        val login = Login(
            squadID = member.squadID,
            squadName = squad.value?.squadName ?: "",
            squadUsername = member.name,
            squadUserId = member.id ?: "",
            phoneNumber = member.phoneNumber,
            role = SquadUserType.SQUAD_MEMBER,
            squadCreatedDate = Date().asTimestamp,
            userCreatedDate = Date().asTimestamp
        )

        manager.addUserLogin(login) { success, error ->
            viewModelScope.launch(Dispatchers.IO) {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    if (login.role == SquadUserType.SQUAD_MEMBER) {
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
        completion: (Boolean, List<Login>?,String?) -> Unit
    ) {
        // ✅ Check Internet
        if (!CommonFunctions.isInternetAvailable()) {

            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.fetchUserLogins(phoneNumber) { loginList, error ->

            // DispatchQueue.main.async → main-safe via viewModelScope
            viewModelScope.launch(Dispatchers.IO) {

                // Guard self check not needed in Kotlin, directly use `this@SquadViewModel`
                try {
                    if (showLoader) {
                        LoaderManager.shared.hideLoader()
                    }

                    if (loginList != null && loginList.isNotEmpty()) {
                        var multipleAccount = false

                        SessionManager.logins = loginList.toMutableList()
                        FirestoreManager.shared.updateFCMTokenForAllUser()

                        if (loginList.size > 1) {
                            multipleAccount = true
                        }
                        else {

                            if (loginList.first().role == SquadUserType.SQUAD_MEMBER) {
                                UserDefaultsManager.saveSquadManagerLogged(false)
                            }
                            else {
                                UserDefaultsManager.saveSquadManagerLogged(true)
                            }

                        }
                        setUsers(loginList)

                        UserDefaultsManager.saveIsMultipleAccount(multipleAccount)
                        completion(true,loginList, null)

                    } else {
                        val errorMessage = error ?: "No squads found for this user"
                        handleFetchError(errorMessage) { }
                        completion(false, emptyList(), errorMessage)
                    }

                } catch (e: Exception) {
                    if (showLoader) {
                        LoaderManager.shared.hideLoader()
                    }
                    completion(false,emptyList(), e.localizedMessage)
                }
            }
        }
    }

    fun fetchSquadByID(
        showLoader: Boolean,
        completion: (Boolean, Squad?, String?) -> Unit
    ) {
        // ✅ Internet check
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        UserDefaultsManager.getLogin()?.let { login ->
            this.squadID = login.squadID.toString()
            this.loginMember = login
            setSelectedUser(login)
        }

        manager.fetchSquadByID(squadID) { fetchedSquad, error ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (fetchedSquad == null) {
                        if (showLoader) LoaderManager.shared.hideLoader()
                        handleFetchError(error ?: "Failed to fetch squad details") {
                            fetchSquadByID(showLoader, completion)
                        }
                        completion(false, null, error ?: "Failed to fetch squad details")
                        return@launch
                    }

                    setSquad(fetchedSquad)
                    setVerifySquadManagerAmountBadgeCount(fetchedSquad.verifyAmountCount)
                    setVerifySquadCashRequestBadgeCount(fetchedSquad.cashRequestedCount)

                    setRemainingMonths(CommonFunctions.getRemainingMonths(
                        startDate = Date(),
                        endDate = fetchedSquad.squadEndDate?.toDate() ?: Date()
                    ))

                    setIsFetchingTotalAmountCollected(true)

                    // 🔹 Parallel background work with coroutine async blocks
                    val fetchEMI = async { fetchEMIConfigurations(true) { _, _ -> } }
                    val fetchMembers = async { fetchMembers(false) { _, _, _ -> } }
                    val fetchPayments = async {
                        manager.fetchPayments(

                            squadID = squadID,

                            memberId = null,

                            lastDocument = null,

                            limit = paymentsPageSize

                        ) { fetchedPayments, lastDoc, error ->
                            viewModelScope.launch(Dispatchers.IO) {
                                if (fetchedPayments != null) {
                                    setSquadPayments(fetchedPayments)
                                    setIsFetchingTotalAmountCollected(false)
                                } else {
                                    val errorMsg = error ?: "❌ Failed to fetch payments"
                                    println(errorMsg)
                                }
                            }
                        }
                    }

                    fetchEMI.await()
                    fetchMembers.await()
                    fetchPayments.await()

                    if (showLoader) LoaderManager.shared.hideLoader()
                    println("✅ All background tasks completed")

                    completion(true, fetchedSquad, null)
                } catch (e: Exception) {
                    if (showLoader) LoaderManager.shared.hideLoader()
                    completion(false, null, e.localizedMessage)
                }
            }
        }
    }


    fun updateSquad(
        showLoader: Boolean = true,
        squad: Squad,
        completion: (Boolean, Squad?, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.updateSquad(squad) { success, updatedSquad, errorMessage ->
            viewModelScope.launch(Dispatchers.IO) {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success && updatedSquad != null) {
                    println("✅ Squad updated successfully!")
                    setSquad(updatedSquad)
                    setRemainingMonths(CommonFunctions.getRemainingMonths(
                        startDate = Date(),
                        endDate = updatedSquad.squadEndDate?.orNow ?: Date()
                    ))

                    completion(true, updatedSquad, null)
                } else {
                    val errorMsg = errorMessage ?: "Unknown error while updating squad"
                    println("❌ $errorMsg")
                    handleFetchError(errorMsg) {
                        updateSquad(showLoader, squad, completion)
                    }
                    completion(false, null, errorMsg)
                }
            }
        }
    }

    fun updateSquadTotalAmount(
        squadId: String,
        amount: Int,
        completion: (Boolean, String?) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {

            LoaderManager.shared.hideLoader()

            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )

            return
        }

        LoaderManager.shared.showLoader()

        manager.updateSquadTotalAmount(
            squadId = squadId,
            amount = amount,
        ) { success, errorMessage ->

            LoaderManager.shared.hideLoader()

            if (success) {

                println("✅ Squad debit/credit updated successfully!")

                _squad.value?.let { currentSquad ->

                    val updatedSquad = currentSquad.copy(
                        currentAvailableAmount = amount
                    )
                    setSquad(updatedSquad)
                }

                completion(true, null)

            } else {

                val errorMsg =
                    errorMessage ?: "Unknown error while updating updateSquadTotalAmount"

                println("❌ $errorMsg")

                completion(false, errorMsg)
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        val squad = _squad.value
        if (squad == null) {
            completion(false, "Squad not found")
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        val newMember = Member(
            id = IDGenerator.generateMemberID(),
            name = name,
            profileImage = "",
            mailID = "",
            phoneNumber = phone,
            password = phone,
            squadID = squad.squadID,
            role = SquadUserType.SQUAD_MEMBER,
            memberCreatedDate = Date().asTimestamp,
            recordStatus = RecordStatus.ACTIVE,
            upiBeneId = "",
            bankBeneId = "",
            upiID = "",
            fcmToken = "",
            currentLoanApproveStatus = EMIStatus.CREATED,
            verifyAmountCount = 0,
            cashRequested = false
        )

        val addAndSave: () -> Unit = {

            setShowAddMemberPopup(false)

            manager.addMember(squadID, newMember) { success, message ->
                viewModelScope.launch(Dispatchers.IO) {
                    if (showLoader) LoaderManager.shared.hideLoader()

                    if (success) {
                        _squadMembers.value += newMember
                        _squadMembersCount.value += 1
                        addUserLogin(false, newMember)
                        completion(true, null)
                    } else {
                        handleFetchError(message ?: "Unknown error") {}
                        completion(false, message ?: "Unknown error")
                    }
                }
            }
        }

        if (_squadMembersCount.value > 0) {
            manager.fetchMembers(squad.squadID) { memberNames, error ->
                viewModelScope.launch(Dispatchers.IO) {
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
                            primaryButtonTitle = SquadStrings.ok,
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
            squadID: String,
            memberID: String,
            completion: (Boolean, Member?, String?) -> Unit
        ) {
            if (!CommonFunctions.isInternetAvailable()) {
                LoaderManager.shared.hideLoader()
                AlertManager.shared.showAlert(
                    title = SquadStrings.appName,
                    message = SquadStrings.networkError,
                    primaryButtonTitle = SquadStrings.ok,
                    primaryAction = {}
                )
                return
            }

            if (showLoader) LoaderManager.shared.showLoader()

            viewModelScope.launch(Dispatchers.IO) { // 🧵 Run on background thread
                manager.fetchMember(squadID, memberID) { fetchedMember, error ->
                    // ✅ Use launch instead of withContext inside callback (no suspend issue)
                    viewModelScope.launch(Dispatchers.Main) { // 🧭 Switch back to Main thread
                        if (showLoader) LoaderManager.shared.hideLoader()

                        if (fetchedMember != null) {
                            // ✅ Update LiveData / StateFlow
                            setCurrentMember(fetchedMember)
                            setVerifySquadMemberAmountBadgeCount(fetchedMember.verifyAmountCount)
                            completion(true, fetchedMember, null)
                        } else {
                            val errorMsg = error ?: "❌ Failed to fetch member"
                            setErrorMessage(errorMsg)

                            // 🔁 Retry logic if error
                            handleFetchError(errorMsg) {
                                fetchMember(showLoader, squadID, memberID, completion)
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
                    primaryButtonTitle = SquadStrings.ok,
                    primaryAction = {}
                )
                return
            }

            if (showLoader) LoaderManager.shared.showLoader()

            viewModelScope.launch(Dispatchers.IO) { // 🧵 Run Firebase fetch in background
                manager.fetchMembers(squadID) { fetchedMembers, error ->
                    // ✅ Launch on Main thread safely for UI updates
                    viewModelScope.launch(Dispatchers.Main) {
                        setIsFetchingMembers(false)
                        if (showLoader) LoaderManager.shared.hideLoader()

                        when {
                            error == "No members found." -> {
                                setSquadMembersCount(0)

                                completion(false, null, error)
                            }

                            fetchedMembers != null -> {

                                setSquadMembers(fetchedMembers)
                                setSquadMembersCount(fetchedMembers.size)
                                setSquadMemberNames(fetchedMembers.map { it.name }.toMutableList())


                                // ✅ Auto-select login member (if applicable)
                                loginMember?.squadUsername?.let { username ->
                                    CommonFunctions.getMemberByName(username, fetchedMembers)?.let {
                                        setMemberDetail(it)

                                    }
                                }

                                completion(true, fetchedMembers, null)
                            }

                            else -> {
                                val errorMsg = error ?: "❌ Failed to fetch members"
                                setErrorMessage(errorMsg)

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
                    primaryButtonTitle = SquadStrings.ok,
                    primaryAction = {}
                )
                return
            }

            val squad = _squad.value ?: return

            manager.createContributionWhenMemberCreate(
                squadID = squad.squadID,
                memberID = member.id ?: "",
                memberName = member.name,
                squadStart = squad.squadStartDate?.toDate() ?: Date(),
                squadEnd = squad.squadEndDate?.toDate() ?: Date(),
                amount = squad.monthlyContribution
            ) { success, message ->
                if (success) {
                    createSquadActivity(
                        activityType = SquadActivityType.OTHER_ACTIVITY,
                        userName = "SQUAD MANAGER",
                        memberId = "",
                        amount = 0,
                        description = "Added a new member ${member.name} to the squad"
                    )
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
            squad: Squad,
            squadEndDate: Date,
            amount: String,
            completion: (Boolean, String?) -> Unit
        ) {
            if (!CommonFunctions.isInternetAvailable()) {
                LoaderManager.shared.hideLoader()
                AlertManager.shared.showAlert(
                    title = SquadStrings.appName,
                    message = SquadStrings.networkError,
                    primaryButtonTitle = SquadStrings.ok,
                    primaryAction = {}
                )
                return
            }

            if (showLoader) LoaderManager.shared.showLoader()

            viewModelScope.launch(Dispatchers.IO) {
                manager.contibutionEditWhenMonthsChanged(
                    squadID = squad.squadID,
                    squadStartDate = squad.squadStartDate?.toDate() ?: Date(),
                    squadEndDate = squadEndDate,
                    amount = squad.monthlyContribution.toString()
                ) { success, message ->
                    // ✅ Switch back to Main thread safely
                    viewModelScope.launch(Dispatchers.Main) {
                        if (showLoader) LoaderManager.shared.hideLoader()

                        if (success) {
                            println(message ?: "✅ Squad contributions updated successfully!")
                            completion(true, message)
                        } else {
                            val errorMsg = message ?: "❌ Error updating squad contributions"
                            println(errorMsg)

                            handleFetchError(errorMsg) {
                                contibutionEditWhenMonthsChanged(
                                    showLoader = showLoader,
                                    squad = squad,
                                    squadEndDate = squadEndDate,
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
        squadID: String,
        memberID: String,
        completion: (List<ContributionDetail>?, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        viewModelScope.launch(Dispatchers.IO) {
            manager.fetchContributionsForMember(squadID, memberID) { contributions, error ->
                // ✅ Switch to main thread safely inside callback
                viewModelScope.launch(Dispatchers.Main) {
                    if (showLoader) LoaderManager.shared.hideLoader()

                    if (error != null) {
                        println(error)
                        handleFetchError(error) {
                            fetchContributionsForMember(
                                showLoader = showLoader,
                                squadID = squadID,
                                memberID = memberID,
                                completion = completion
                            )
                        }
                        completion(null, error)
                    } else {
                        println("✅ Contributions fetched successfully")
                        contributions?.let {

                            setSelectedContributions(it)
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
        squadID: String,
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        viewModelScope.launch(Dispatchers.IO) {
            manager.editContribution(
                squadID = squadID,
                memberID = memberID,
                contributionID = contributionID,
                updatedContribution = updatedContribution
            ) { success, error ->
                // ✅ Move UI updates safely to Main thread
                viewModelScope.launch(Dispatchers.Main) {
                    if (showLoader) LoaderManager.shared.hideLoader()

                    if (success) {
                        println("✅ Squad contributions updated successfully!")
                        completion(true, "✅ Squad contributions updated successfully!")
                    } else {
                        val errorMsg = error ?: "❌ Error updating squad contributions"
                        println(errorMsg)

                        handleFetchError(errorMsg) {
                            editContribution(
                                showLoader = showLoader,
                                squadID = squadID,
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

    // ✅ addSquadActivity
    fun addSquadActivity(
        showLoader: Boolean,
        activity: SquadActivity,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        viewModelScope.launch(Dispatchers.IO) {
            manager.addSquadActivity(squad.value?.squadID ?: "", activity) { success, error ->
                // ✅ Switch safely to main thread inside callback
                viewModelScope.launch(Dispatchers.Main) {
                    if (showLoader) LoaderManager.shared.hideLoader()

                    if (success) {
                        _squadActivities.value += activity
                        completion(true, null)
                    } else {
                        val errorMsg = error ?: "❌ Failed to append squad activity"
                        println(errorMsg)

                        handleFetchError(errorMsg) {
                            addSquadActivity(
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

    fun fetchSquadActivities(
        squadID: String,
        memberId: String? = null,
        showLoader: Boolean = true,
        completion: (Boolean, String?) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {

            LoaderManager.shared.hideLoader()

            completion(false, SquadStrings.networkError)

            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        activitiesIsLoadingMore = true
        isLoadingMoreActivities = true

        manager.fetchSquadActivities(
            squadID = squadID,
            memberId = memberId,
            lastDocument = null,
            limit = activitiesPageSize
        ) { activities, lastDocument, error ->

            MainScope().launch {

                LoaderManager.shared.hideLoader()

                activitiesIsLoadingMore = false
                isLoadingMoreActivities = false

                if (activities != null) {

                    setSquadActivities(activities)


                    activitiesLastDocument = lastDocument

                    activitiesHasMoreData =
                        activities.size == activitiesPageSize

                    completion(true, null)

                } else {

                    completion(
                        false,
                        error ?: "Failed to fetch activities"
                    )
                }
            }
        }
    }

    fun resetActivitiesPagination() {

        setSquadActivities(emptyList())

        activitiesLastDocument = null

        activitiesHasMoreData = true

        activitiesIsLoadingMore = false

        isLoadingMoreActivities = false
    }

    fun loadMoreActivities(
        squadID: String,
        memberId: String? = null
    ) {

        if (!activitiesHasMoreData) return

        if (activitiesIsLoadingMore) return

        val lastDocument = activitiesLastDocument ?: return

        activitiesIsLoadingMore = true
        isLoadingMoreActivities = true

        manager.fetchSquadActivities(
            squadID = squadID,
            memberId = memberId,
            lastDocument = lastDocument,
            limit = activitiesPageSize
        ) { activities, newLastDocument, _ ->

            MainScope().launch {

                activitiesIsLoadingMore = false
                isLoadingMoreActivities = false

                if (activities != null) {

                    val updated =
                        _squadActivities.value.toMutableList()

                    updated.addAll(activities)

                    setSquadActivities(updated)

                    activitiesLastDocument = newLastDocument

                    activitiesHasMoreData =
                        activities.size == activitiesPageSize
                }
            }
        }
    }

    fun loadMoreActivitiesIfNeeded(
        currentActivity: SquadActivity,
        squadID: String,
        memberId: String? = null
    ) {

        val lastActivity =
            _squadActivities.value.lastOrNull()
                ?: return

        if (currentActivity.id != lastActivity.id) {
            return
        }

        loadMoreActivities(
            squadID = squadID,
            memberId = memberId
        )
    }

    fun deleteSquadActivity(showLoader: Boolean = true, activityID: String) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.deleteSquadActivity(squadID, activityID) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    setSquadActivities(_squadActivities.value.filter { it.squadID != activityID })

                } else {
                    val errorMsg = error ?: "❌ Failed to delete squad activity"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        deleteSquadActivity(showLoader, activityID)
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.fetchSquadRules(squadID) { rules, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (rules != null) {
                    setRules(rules)
                } else if (error != null) {
                    handleFetchError(error) {
                        fetchRules(showLoader)
                    }
                }
            }
        }
    }

    fun addRule(rule: SquadRule, showLoader: Boolean = true, completion: (Boolean, String?) -> Unit) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.addSquadRule(squadID, rule) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    _rules.value += rule
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to add squad rule"
                    handleFetchError(errorMsg) {
                        addRule(rule, showLoader, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun deleteRule(rule: SquadRule, showLoader: Boolean = true, completion: (Boolean, String?) -> Unit) {
        val ruleID = rule.id ?: run {
            completion(false, "❌ Invalid rule ID")
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.deleteSquadRule(squadID, ruleID) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    setRules(_rules.value.filter { it.id != ruleID })

                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to delete squad rule"
                    handleFetchError(errorMsg) {
                        deleteRule(rule, showLoader, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun updateRule(rule: SquadRule, completion: (Boolean, String?) -> Unit) {
        val currentSquadID = _squad.value?.squadID
        if (currentSquadID == null) {
            completion(false, "Squad ID not found.")
            return
        }

        manager.updateSquadRule(currentSquadID, rule) { success, error ->
            MainScope().launch {
                if (success) {
                    val index = _rules.value.indexOfFirst { it.id == rule.id }
                    if (index != -1) setRules(_rules.value.toMutableList().apply { set(index, rule) })
                    completion(true, null)
                } else {
                    completion(false, error ?: "Failed to update rule.")
                }
            }
        }
    }

    fun updateContributionStatus(
        showLoader: Boolean = true,
        squadID: String,
        memberID: String,
        contributionID: String,
        amount : Int,
        newStatus: String,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateContributionStatus(squadID, memberID, contributionID,amount,newStatus) { success, message ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (!success) {
                val errorMsg = message ?: "❌ Failed to update contribution status"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    updateContributionStatus(showLoader, squadID, memberID, contributionID, amount,newStatus, completion)
                }
                completion(false, errorMsg)
                return@updateContributionStatus
            }

            val index = _selectedContributions.value.indexOfFirst { it.id == contributionID && it.memberID == memberID }
            if (index != -1) {
                setSelectedContributions(_selectedContributions.value.toMutableList().apply {
                    this[index] = this[index].copy(
                        paidStatus = PaidStatus.PAID,
                        paidOn = Timestamp(Date())
                    )
                })
            }

            completion(true, message)
        }
    }

    fun updateLoanStatus(
        squadID: String,
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(false, SquadStrings.networkError)
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateLoanStatus(squadID, memberID, loanID, status) { success, message ->
            if (showLoader) LoaderManager.shared.hideLoader()
            completion(success, message)
        }
    }

    fun updateLoanAndAllInstallmentsStatus(
        squadID: String,
        memberID: String,
        loanID: String,
        status: String,
        showLoader: Boolean = true,
        completion: (Boolean, String?) -> Unit
    ) {

        // 1. Internet check
        if (!CommonFunctions.isInternetAvailable()) {
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(false, SquadStrings.networkError)
            return
        }

        // 2. Show loader
        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        // 3. Call manager
        manager.updateLoanAndAllInstallmentsStatus(
            squadID,
            memberID,
            loanID,
            status
        ) { success, message ->

            // 4. Hide loader
            if (showLoader) {
                LoaderManager.shared.hideLoader()
            }

            // 5. Return result
            completion(success, message)
        }
    }

    fun updateInstallmentStatus(
        squadID: String,
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(false, SquadStrings.networkError)
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateInstallmentStatus(
            squadID = squadID,
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
        squadID: String,
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updatePaymentStatus(
            squadID = squadID,
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
                    updatePaymentStatus(showLoader, squadID, paymentID, status, reason, completion)
                }
                completion(false, errorMsg)
                return@updatePaymentStatus
            }

            val index = _squadPayments.value.indexOfFirst { it.id == paymentID }
            if (index != -1) {
                println("SavingsSquadPayment local cache updated")

                setSquadPayments(_squadPayments.value.toMutableList().apply {
                    val updatedPayment = this[index].copy(
                        paymentStatus = if (status == "SUCCESS") PaymentStatus.SUCCESS else PaymentStatus.FAILED,
                        paymentUpdatedDate = Timestamp(Date())
                    )
                    this[index] = updatedPayment
                })
            }

            completion(true, null)
        }
    }

    fun savePayments(
        activity: Activity,
        context: Context,
        showUPIIntent: Boolean = true,
        showLoader: Boolean = true,
        squadID: String,
        payment: List<PaymentsDetails>,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        val firstPayment = payment.firstOrNull()

        if (firstPayment != null &&
            firstPayment.paymentApproveStatus != PaymentApproveStatus.ACCEPTED && showUPIIntent
        ) {

            // Save pending payment
            LoaderManager.shared.hideLoader()

            UPIPaymentManager.shared.pay(
                activity =  activity,
                context = context,
                upiID = firstPayment.upiID,
                name = squad.value?.squadName ?: "",
                amount = firstPayment.amount.toDouble(),
                note = firstPayment.description,
                transactionRef = "TXN_${firstPayment.id ?: UUID.randomUUID().toString()}",
                completion = { initiated ->
                    println("Initiated: $initiated")
                    if (initiated) {

                        UserDefaultsManager.savePendingPayment(firstPayment)
                        completion(true, "UPI_OPENED")
                    }
                },
                onReturn = { status ->
                    when (status) {
                        UPIPaymentStatus.SUCCESS -> {
                            println("✅ Success")
                        }

                        UPIPaymentStatus.FAILED -> {
                            println("❌ Failed")
                        }

                        UPIPaymentStatus.PENDING -> {
                            println("⏳ Pending — verify with backend")
                        }

                        UPIPaymentStatus.CANCELLED -> {
                            println("🚫 Cancelled")
                        }
                    }
                }
            )

            return
        }

        if (firstPayment?.paymentSubType == PaymentSubType.OTHERS_AMOUNT || firstPayment?.paymentEntryType == PaymentEntryType.MANUAL_ENTRY) {
            firstPayment.paymentApproveStatus = PaymentApproveStatus.ACCEPTED
            firstPayment.paymentStatus = PaymentStatus.SUCCESS
            firstPayment.paymentUpdatedDate = Date().asTimestamp
        }
        manager.savePayments(squadID, firstPayment) { success, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (!success) {
                val errorMsg = error ?: "❌ Failed to add payment"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    savePayments(activity,context,showUPIIntent, showLoader, squadID, payment, completion)
                }
                completion(false, errorMsg)
                return@savePayments
            }

            setSquadPayments(_squadPayments.value.toMutableList().apply {
                addAll(payment)
            })


            for (payment in payment) {

                if (payment.paymentSubType == PaymentSubType.CONTRIBUTION_AMOUNT) {

                    if (payment.paymentEntryType == PaymentEntryType.MANUAL_ENTRY) {

                        updateContributionStatus(
                            squadID = payment.squadId,
                            memberID = payment.memberId,
                            contributionID = payment.contributionId,
                            amount = payment.amount,
                            newStatus = PaidStatus.PAID.value
                        ) { success, error ->

                            // handle response if needed
                            if (!success) {
                                println("Error updating: $error")
                            }
                        }

                    }
                    else {

                        updateContributionStatus(
                            squadID = payment.squadId,
                            memberID = payment.memberId,
                            contributionID = payment.contributionId,
                            amount = payment.amount,
                            newStatus = PaidStatus.INVERIFICATION.value
                        ) { success, error ->

                            // handle response if needed
                            if (!success) {
                                println("Error updating: $error")
                            }
                        }
                    }


                }
                else if (payment.paymentSubType == PaymentSubType.EMI_AMOUNT) {

                    if (payment.paymentEntryType == PaymentEntryType.MANUAL_ENTRY) {

                        updateInstallmentStatus(squadID = payment.squadId, memberID = payment.memberId, loanID = payment.loanId, installmentID = payment.installmentId, status = EMIStatus.PAID.value){ success, error ->
                            // handle response if needed
                            if (!success) {
                                println("Error updating: $error")
                            }
                            else {

                                updateLoanPaidAfterInstallmentSettled(_memberPendingLoans.value ?: emptyList(), payment.memberId)

                            }
                        }
                    }
                    else {

                        updateInstallmentStatus(squadID = payment.squadId, memberID = payment.memberId, loanID = payment.loanId, installmentID = payment.installmentId, status = EMIStatus.INVERIFICATION.value){ success, error ->
                            // handle response if needed
                            if (!success) {
                                println("Error updating: $error")
                            }
                            else {

                                updateLoanPaidAfterInstallmentSettled(_memberPendingLoans.value ?: emptyList(), payment.memberId)

                            }
                        }
                    }

                }
                else if (payment.paymentSubType == PaymentSubType.LOAN_AMOUNT) {

                    FirestoreManager.shared.updateCurrentLoanApproveStatus(payment.squadId,payment.memberId,
                        EMIStatus.INVERIFICATION) {_,_ -> }
                }

                if (payment.paymentSubType == PaymentSubType.OTHERS_AMOUNT || payment.paymentEntryType == PaymentEntryType.MANUAL_ENTRY) {
                    updatePaymentCalculations(listOf(payment), PaymentApproveStatus.ACCEPTED)

                    if (payment.paymentEntryType == PaymentEntryType.MANUAL_ENTRY) {

                        val message = when (payment.paymentSubType) {
                            PaymentSubType.CONTRIBUTION_AMOUNT ->
                                "Squad Manager updated your contribution for ${payment.transferReferenceId}"

                            PaymentSubType.EMI_AMOUNT ->
                                "Squad Manager updated your EMI for ${payment.transferReferenceId}"

                            else -> ""
                        }

                        if (message.isNotEmpty()) {

                            NotificationService.shared.sendMemberReminder(

                                request = ReminderRequest(

                                    squadId = squad.value?.squadID
                                        ?: "",

                                    memberIds = listOf(payment.memberId) ,

                                    title = "Payment Updated",

                                    message = message,

                                    data = mapOf(

                                        "screen" to "PAYMENT"

                                    )

                                ),

                                onSuccess = { response ->
                                    LoaderManager.shared.hideLoader()
                                    ToastManager.show(
                                        title = "Reminder Sent",
                                        message = "Notification sent to ${response.sentTo} member(s)",
                                        type = ToastType.SUCCESS
                                    )
                                },

                                onError = { error ->
                                    LoaderManager.shared.hideLoader()
                                    ToastManager.show(
                                        title = "Failed",
                                        message = error.localizedMessage ?: "Unable to send reminder.",
                                        type = ToastType.ERROR
                                    )
                                }

                            )

                        }
                    }
                }
            }

            if (showLoader) LoaderManager.shared.showLoader()
            completion(true, null)
        }
    }

    fun updateContributionApproveStatus(
        showLoader: Boolean = true,
        squadID: String,
        memberID: String,
        contributionID: String,
        status: PaidStatus,
        completion: (Boolean, ContributionDetail?, String?) -> Unit
    ) {

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.updateContributionApproveStatus(
            squadID = squadID,
            memberID = memberID,
            contributionID = contributionID,
            status = status
        ) { success, contribution, error ->

            if (showLoader) {
                LoaderManager.shared.hideLoader()
            }

            if (!success) {
                completion(false, null, error)
                return@updateContributionApproveStatus
            }

            completion(true, contribution, null)
        }
    }

    fun updatePaymentCalculations(payment: List<PaymentsDetails>, status: PaymentApproveStatus) {
        val squadLocal = _squad.value ?: return
        val userId = payment.firstOrNull()?.memberId
        val member = _squadMembers.value.firstOrNull { it.id == userId }

        CoroutineScope(Dispatchers.IO).launch {
            // 🔹 Update squad financials atomically
            for (pay in payment) {
                applyPaymentToFirestore(squadID = squadLocal.squadID, payment = pay, status = status)

                if (status == PaymentApproveStatus.ACCEPTED && pay.memberId.isNotEmpty()) {

                    applyMemberToFirestore(

                        squadID = squadLocal.squadID,

                        memberID = pay.memberId,

                        payment = pay,

                        status = status

                    )
                }


            }

        }
    }

    fun applyMemberToFirestore(
        squadID: String,
        memberID: String,
        payment: PaymentsDetails,
        status: PaymentApproveStatus
    ) {

        if (status != PaymentApproveStatus.ACCEPTED) return

        val updates = mutableMapOf<String, Any>()

        when (payment.paymentType) {

            PaymentType.PAYMENT_CREDIT -> {

                when (payment.paymentSubType) {

                    PaymentSubType.CONTRIBUTION_AMOUNT -> {
                        updates["totalContributionPaid"] =
                            FieldValue.increment(payment.amount.toLong())
                    }

                    PaymentSubType.INTEREST_AMOUNT -> {
                        updates["totalInterestPaid"] =
                            FieldValue.increment(payment.intrestAmount.toLong())
                    }

                    PaymentSubType.EMI_AMOUNT -> {

                        updates["totalLoanPaid"] =
                            FieldValue.increment((payment.amount).toLong())

                        updates["totalInterestPaid"] =
                            FieldValue.increment(payment.intrestAmount.toLong())
                    }

                    else -> Unit
                }
            }

            PaymentType.PAYMENT_DEBIT -> {

                if (payment.paymentSubType == PaymentSubType.LOAN_AMOUNT) {

                    updates["totalLoanBorrowed"] =
                        FieldValue.increment((payment.amount - payment.intrestAmount).toLong())
                }
            }
        }

        if (updates.isEmpty()) return

        FirebaseFirestore.getInstance()
            .collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .update(updates)
            .addOnSuccessListener {
                println("✅ Member financials updated atomically")
            }
            .addOnFailureListener {
                println("❌ Failed to update member financials: ${it.message}")
            }
    }

    fun applyPaymentToFirestore(squadID: String, payment: PaymentsDetails, status: PaymentApproveStatus) {
        if (status != PaymentApproveStatus.ACCEPTED) return

        val updates = mutableMapOf<String, Any>()

        when (payment.paymentType) {
            PaymentType.PAYMENT_CREDIT -> {
                when (payment.paymentSubType) {
                    PaymentSubType.CONTRIBUTION_AMOUNT -> {
                        updates["totalContributionAmountReceived"] = FieldValue.increment(payment.amount.toLong())
                        updates["currentCreditAmount"] = FieldValue.increment(payment.amount.toLong())
                        updates["currentAvailableAmount"] = FieldValue.increment(payment.amount.toLong())
                    }
                    PaymentSubType.INTEREST_AMOUNT -> {
                        updates["totalInterestAmountReceived"] = FieldValue.increment(payment.intrestAmount.toLong())
                        updates["currentCreditAmount"] = FieldValue.increment(payment.intrestAmount.toLong())
                        updates["currentAvailableAmount"] = FieldValue.increment(payment.amount.toLong())
                    }
                    PaymentSubType.EMI_AMOUNT -> {
                        updates["totalLoanAmountReceived"] = FieldValue.increment((payment.amount.toLong()))
                        updates["totalInterestAmountReceived"] = FieldValue.increment(payment.intrestAmount.toLong())
                        updates["currentCreditAmount"] = FieldValue.increment((payment.amount + payment.intrestAmount).toLong())
                        updates["currentAvailableAmount"] = FieldValue.increment((payment.amount + payment.intrestAmount).toLong())
                    }
                    else -> {
                        updates["currentCreditAmount"] = FieldValue.increment(payment.amount.toLong())
                        updates["currentAvailableAmount"] = FieldValue.increment(payment.amount.toLong())
                    }
                }
            }
            PaymentType.PAYMENT_DEBIT -> {
                if (payment.paymentSubType == PaymentSubType.LOAN_AMOUNT) {
                    updates["totalLoanAmountSent"] = FieldValue.increment((payment.amount - payment.intrestAmount).toLong())
                }
                updates["currentDebitAmount"] = FieldValue.increment((payment.amount - payment.intrestAmount).toLong())
                updates["currentAvailableAmount"] = FieldValue.increment((-payment.amount).toLong())
            }
        }

        FirebaseFirestore.getInstance()
            .collection("squads")
            .document(squadID)
            .update(updates)
            .addOnSuccessListener { println("✅ Squad financials updated atomically") }
            .addOnFailureListener { println("❌ Failed to update squad financials: ${it.message}") }
    }

    fun updatePaymentApproveStatus(
        showLoader: Boolean = true,
        squadID: String,
        paymentID: String,
        status: PaymentApproveStatus,
        completion: (Boolean, String?) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError
            )
            completion(false, "No internet connection")
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updatePaymentApproveStatus(
            squadID = squadID,
            paymentID = paymentID,
            status = status
        ) { success, payment, error ->

            if (success && payment != null) {

                val list = (squadPayments.value ?: mutableListOf()).toMutableList()

                val index = list.indexOfFirst { it.id == paymentID }
                if (index != -1) {
                    list[index] = list[index].copy(
                        paymentApproveStatus = status
                    )
                }

                setSquadPayments(list)

                // remove from pending list
                val pending = pendingApprovalPayments.value.toMutableList() ?: mutableListOf()

                pending.removeAll { it.id == paymentID }

                setPendingApprovalPayments(pending)

                updatePaymentCalculations(listOf(payment), status)

                // contribution logic
                if (payment.paymentSubType == PaymentSubType.CONTRIBUTION_AMOUNT) {

                    when (status) {

                        PaymentApproveStatus.ACCEPTED -> {
                            updateContributionStatus(
                                squadID = payment.squadId,
                                memberID = payment.memberId,
                                contributionID = payment.contributionId,
                                amount = payment.amount,
                                newStatus = PaidStatus.PAID.value
                            ){ success, error ->

                                if (success) {

                                    // success logic

                                } else {

                                    // show error

                                    println(error)

                                }

                            }
                        }

                        PaymentApproveStatus.REJECTED -> {
                            updateContributionStatus(
                                squadID = payment.squadId,
                                memberID = payment.memberId,
                                contributionID = payment.contributionId,
                                amount = payment.amount,
                                newStatus = PaidStatus.NOT_PAID.value
                            ){ success, error ->

                                if (success) {

                                    // success logic

                                } else {

                                    // show error

                                    println(error)

                                }

                            }
                        }

                        else -> {}
                    }
                }
                else if (payment.paymentSubType == PaymentSubType.EMI_AMOUNT) {

                    when (status) {

                        PaymentApproveStatus.ACCEPTED -> {
                            updateInstallmentStatus(squadID = payment.squadId, memberID = payment.memberId, loanID = payment.loanId, installmentID = payment.installmentId, status = EMIStatus.PAID.value){ success, error ->
                                // handle response if needed
                                if (!success) {
                                    println("Error updating: $error")
                                }
                                else {


                                }
                            }
                        }

                        PaymentApproveStatus.REJECTED -> {
                            updateInstallmentStatus(squadID = payment.squadId, memberID = payment.memberId, loanID = payment.loanId, installmentID = payment.installmentId, status = EMIStatus.PENDING.value){ success, error ->
                                // handle response if needed
                                if (!success) {
                                    println("Error updating: $error")
                                }
                                else {

                                }
                            }
                        }

                        else -> {}
                    }


                }
                else if (payment.paymentSubType == PaymentSubType.LOAN_AMOUNT) {

                    when (status) {

                        PaymentApproveStatus.ACCEPTED -> {

                            val emiConfig = payment.selectedEMIConfig ?: return@updatePaymentApproveStatus

                            val newLoan = CommonFunctions.generateMemberLoan(
                                emiConfig = emiConfig,
                                memberID = payment.memberId ?: "",
                                memberName = payment.memberName
                            )

                            newLoan.id = payment.loanId

                            addOrUpdateMemberLoan(
                                showLoader = false,
                                memberID = payment.memberId ?: "",
                                loan = newLoan
                            ) { success, error ->
                                if (success) {
                                    LoaderManager.shared.hideLoader()


                                    /*FirebaseFunctionsManager.shared.processRazorPayPayment(
                                        squadId = squadViewModel.squad.value?.squadID ?: "",
                                        action = RazorpayPaymentAction.New(payment = newPayment)
                                    ) { sessionId, orderId, error ->

                                        squadViewModel.handleCashFreeResponse(
                                            sessionId, orderId, error,
                                            completion = {
                                                LoaderManager.shared.hideLoader()
                                                handler()
                                            }
                                        )
                                    } */
                                } else {
                                    println("❌ Error: ${error ?: "Unknown error"}")
                                }
                            }

                            FirestoreManager.shared.updateCurrentLoanApproveStatus(payment.squadId,payment.memberId,
                                EMIStatus.PENDING) {_,_ -> }
                        }

                        PaymentApproveStatus.REJECTED -> {

                            FirestoreManager.shared.updateCurrentLoanApproveStatus(payment.squadId,payment.memberId,
                                EMIStatus.CREATED) {_,_ -> }
                        }

                        else -> {}
                    }


                }

                if (showLoader) LoaderManager.shared.hideLoader()

                completion(true, null)
                return@updatePaymentApproveStatus
            }

            val errorMsg = error ?: "Failed to update approval status"
            completion(false, errorMsg)
        }
    }

    fun fetchPendingApprovalPayments(

        showLoader: Boolean = true,

        screenType: SquadUserType,

        memberId: String? = null,

        completion: (Boolean, String?) -> Unit

    ) {

        val squadID = squad.value?.squadID ?: run {

            completion(false, "Squad not found")

            return

        }

        if (!CommonFunctions.isInternetAvailable()) {

            completion(false, "No internet")

            return

        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.fetchPendingApprovals(

            squadID = squadID,

            screenType = screenType,

            memberId = memberId

        ) { list, error ->

            LoaderManager.shared.hideLoader()

            if (error != null) {

                completion(false, error)

                return@fetchPendingApprovals

            }

            setPendingApprovalPayments( list ?: emptyList())
            completion(true, null)

        }

    }

    fun updateMembers(
        showLoader: Boolean = true,
        squadID: String,
        members: List<Member>,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateMembers(squadID, members) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    println("✅ Members updated successfully!")
                    for (updatedMember in members) {
                        val currentList = _squadMembers.value.toMutableList()
                        val index = currentList.indexOfFirst { it.id == updatedMember.id }
                        if (index != -1) {
                            currentList[index] = updatedMember
                            setSquadMembers(currentList)
                        }
                    }

                    setSquadMembersCount(_squadMembers.value.size)
                    setSquadMemberNames(_squadMembers.value.map { it.name })

                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to update members"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        updateMembers(showLoader, squadID, members, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun updateMemberMobileNumber(
        showLoader: Boolean,
        squadID: String,
        memberID: String,
        mobileNumber: String,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        val selectedUser = selectedUser.value ?: return
        manager.updateMemberMobileNumber(squadID, memberID, mobileNumber,selectedUser) { success, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to updateMemberMobileNumber"
                    println(errorMsg)
                    handleFetchError(errorMsg) {
                        updateMemberMobileNumber(showLoader, squadID, memberID, mobileNumber, completion)
                    }
                    completion(false, errorMsg)
                }
            }
        }
    }

    fun updateMemberAmount(
        showLoader: Boolean,
        squadID: String,
        memberID: String,
        amount: Int,
        editAmountType: AmountEditType,
        completion: (Boolean, String?) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.updateMemberAmount(
            squadID = squadID,
            memberID = memberID,
            amount = amount,
            editAmountType = editAmountType
        ) { success, error ->

            MainScope().launch {

                if (showLoader) {
                    LoaderManager.shared.hideLoader()
                }

                if (success) {

                    currentMember.value?.let { member ->

                        when (editAmountType) {

                            AmountEditType.contribution -> {


                                createSquadActivity(
                                    activityType = SquadActivityType.AMOUNT_EDIT,
                                    userName = "SQUAD MANAGER",
                                    memberId = memberID,
                                    amount = amount,
                                    description = "Manager updated member contribution ${currentMember.value?.totalContributionPaid ?: 0} to $amount")

                                member.totalContributionPaid = amount
                            }

                            AmountEditType.loanBorrowed -> {

                                createSquadActivity(
                                    activityType = SquadActivityType.AMOUNT_EDIT,
                                    userName = "SQUAD MANAGER",
                                    memberId = memberID,
                                    amount = amount,
                                    description = "Manager updated member loan borrowed ${currentMember.value?.totalLoanBorrowed ?: 0} to $amount")

                                member.totalLoanBorrowed = amount
                            }

                            AmountEditType.paidLoadAmount -> {

                                createSquadActivity(
                                    activityType = SquadActivityType.AMOUNT_EDIT,
                                    userName = "SQUAD MANAGER",
                                    memberId = memberID,
                                    amount = amount,
                                    description = "Manager updated member loan paid ${currentMember.value?.totalLoanPaid ?: 0} to $amount")

                                member.totalLoanPaid = amount
                            }

                            AmountEditType.intrestAmount -> {

                                createSquadActivity(
                                    activityType = SquadActivityType.AMOUNT_EDIT,
                                    userName = "SQUAD MANAGER",
                                    memberId = memberID,
                                    amount = amount,
                                    description = "Manager updated member pain interest ${currentMember.value?.totalInterestPaid ?: 0} to $amount")

                                member.totalInterestPaid = amount
                            }

                            else -> {}
                        }

                        setCurrentMember(member)
                    }

                    completion(true, null)

                } else {

                    val errorMsg = error ?: "❌ Failed to updateMemberAmount"

                    println(errorMsg)

                    handleFetchError(errorMsg) {
                        updateMemberAmount(
                            showLoader = showLoader,
                            squadID = squadID,
                            memberID = memberID,
                            amount = amount,
                            editAmountType = editAmountType,
                            completion = completion
                        )
                    }

                    completion(false, errorMsg)
                }
            }
        }
    }

    fun fetchPayments(
        showLoader: Boolean,
        memberId: String? = null,
        filterType : PaymentFilter = PaymentFilter.ALL,
        showRejected : Boolean = true,
        completion: (Boolean, String?) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            completion(false, "No Internet")
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        paymentsIsLoadingMore = true

        manager.fetchPayments(
            squadID = squadID,
            memberId = memberId,
            filterType = filterType,
            lastDocument = null,
            limit = paymentsPageSize,
            showRejected = showRejected
        ) { payments, lastDoc, error ->

            paymentsIsLoadingMore = false
            LoaderManager.shared.hideLoader()

            if (payments != null) {

                setSquadPayments(payments)

                paymentsLastDocument = lastDoc

                paymentsHasMoreData =
                    payments.size == paymentsPageSize

                completion(true, null)

            } else {

                completion(false, error ?: "Failed to fetch payments")
            }
        }
    }

    fun resetPaymentsPagination() {

        setSquadPayments(emptyList())

        paymentsLastDocument = null

        paymentsHasMoreData = true

        paymentsIsLoadingMore = false
    }

    fun loadMorePayments(memberId: String? = null, filter: PaymentFilter) {

        if (paymentsIsLoadingMore) return
        if (!paymentsHasMoreData) return
        val last = paymentsLastDocument ?: return

        paymentsIsLoadingMore = true

        manager.fetchPayments(
            squadID = squadID,
            memberId = memberId,
            filter,
            lastDocument = last,
            limit = paymentsPageSize
        ) { payments, newLastDoc, _ ->

            paymentsIsLoadingMore = false

            if (payments != null) {

                val current = squadPayments.value.toMutableList()
                current.addAll(payments)
                setSquadPayments(current)

                paymentsLastDocument = newLastDoc

                paymentsHasMoreData =
                    payments.size == paymentsPageSize
            }
        }
    }

    fun loadMorePaymentsIfNeeded(
        currentPayment: PaymentsDetails,
        filterType: PaymentFilter = PaymentFilter.ALL,
        memberId: String? = null,
    ) {

        val last = squadPayments.value.lastOrNull() ?: return

        if (currentPayment.id != last.id) return

        loadMorePayments(memberId,filterType)
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.addEMIConfiguration(squadID, emi) { success, error ->
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
        squadID: String,
        emi: EMIConfiguration,
        completion: (Boolean, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.addOrUpdateEMIConfiguration(squadID, emi) { success, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (success) {
                println("✅ EMI Configuration added/updated successfully!")

                    setEMIConfigurations(_emiConfigurations.value.toMutableList().apply {
                        val index = indexOfFirst { it.id == emi.id }
                        if (index != -1) {
                            this[index] = emi // Update existing item
                        } else {
                            add(emi) // Add new item
                        }
                    })

                completion(true, null)
            } else {
                val errorMsg = error ?: "❌ Failed to add/update EMI configuration"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    addOrUpdateEMIConfiguration(showLoader, squadID, emi, completion)
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.fetchEMIConfigurations(squadID) { emiList, error ->
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.deleteEMIConfiguration(squadID, emiID) { success, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (success) {
                    setEMIConfigurations(_emiConfigurations.value.filter { it.id != emiID })
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

    fun fetchMemberLoans(
        showLoader: Boolean,
        memberID: String,
        completion: (Boolean, String?) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {

            if (showLoader) {
                LoaderManager.shared.hideLoader()
            }

            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )

            completion(false, SquadStrings.networkError)
            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.fetchMemberLoans(squadID, memberID) { loans, error ->

            if (loans == null) {

                if (showLoader) {
                    LoaderManager.shared.hideLoader()
                }

                val errorMsg = error ?: "Failed to fetch loans"

                handleFetchError(errorMsg) {
                    fetchMemberLoans(
                        showLoader,
                        memberID,
                        completion
                    )
                }

                completion(false, errorMsg)
                return@fetchMemberLoans
            }

            setMemberLoans(loans)

            FirestoreManager.shared.fetchMemberPendingLoans(
                squadID,
                memberID
            ) { pendingLoans, pendingError ->

                setMemberPendingLoans(
                    pendingLoans ?: emptyList()
                )
                if (showLoader) {
                    LoaderManager.shared.hideLoader()
                }

                if (pendingError != null) {
                    completion(false, pendingError)
                } else {
                    completion(true, null)
                }
            }
        }
    }

    private fun updateLoanPaidAfterInstallmentSettled(loans: List<MemberLoan>, memberID: String) {
        if (loans.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }
        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.addOrUpdateMemberLoan(squadID, memberID, loan) { success, error ->
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

    fun fetchAllLoansInSquad(
        showLoader: Boolean = true,
        squadID: String,
        completion: (Boolean, List<MemberLoan>?, String?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = SquadStrings.networkError,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }
        if (showLoader) LoaderManager.shared.showLoader()

        val db = FirebaseFirestore.getInstance()
        val membersRef = db.collection("squads").document(squadID).collection("members")
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }
        if (showLoader) LoaderManager.shared.showLoader()

        manager.deleteMemberLoan(squadID, memberID, loanID) { success, error ->
            Handler(Looper.getMainLooper()).post {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (success) {
                    setMemberLoans(_memberLoans.value.filter { it.id != loanID })
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }
        if (showLoader) LoaderManager.shared.showLoader()

        manager.addOrUpdateInstallment(squadID, memberID, loanID, installment) { success, error ->
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }
        if (showLoader) LoaderManager.shared.showLoader()

        manager.removeInstallment(squadID, memberID, loanID, installment) { success, error ->
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
            primaryButtonTitle = SquadStrings.ok,
            primaryAction = {},
            secondaryButtonTitle = "Retry",
            secondaryAction = retryAction
        )
    }

    fun createSquadActivity(
        activityType: SquadActivityType,
        userName: String,
        memberId: String,
        amount: Int,
        description: String,
        completion: ((Boolean, String?) -> Unit)? = null
    ) {
        val squad = _squad.value ?: run {
            println("❌ No squad found!")
            completion?.invoke(false, "No squad found")
            return
        }

        val activity = SquadActivity(
            squadID = squad.squadID,
            squadName = squad.squadName,
            memberId = memberId,
            date = Timestamp.now(),
            activityType = activityType,
            userName = userName,
            amount = amount,
            description = description
        )

        addSquadActivity(true, activity) { success, error ->
            if (success) {
                println("✅ Activity added successfully!")
                completion?.invoke(true, null)
            } else {
                println("❌ Failed to add activity: $error")
                completion?.invoke(false, error)
            }
        }
    }

    fun fetchDueContributionsAndInstallments(
        squadID: String,
        completion: (List<ContributionDetail>, List<Installment>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val membersRef = db.collection("squads").document(squadID).collection("members")

        val dueContributions = mutableListOf<ContributionDetail>()
        val dueInstallments = mutableListOf<Installment>()

        membersRef.get()
            .addOnSuccessListener { snapshot ->

                val members = snapshot.documents

                if (members.isEmpty()) {
                    completion(emptyList(), emptyList())
                    return@addOnSuccessListener
                }

                // Two Firestore requests per member:
                // 1. contributions
                // 2. loans
                val latch = CountDownLatch(members.size * 2)

                val formatter = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)

                for (memberDoc in members) {

                    val memberID = memberDoc.id

                    val contribRef =
                        membersRef.document(memberID).collection("contributions")

                    val loansRef =
                        membersRef.document(memberID).collection("loans")

                    // ---------------- Contributions ----------------

                    contribRef.get()
                        .addOnSuccessListener { contribSnap ->

                            for (doc in contribSnap.documents) {

                                doc.toObject(ContributionDetail::class.java)?.let { contribution ->

                                    val dueDate = try {
                                        formatter.parse(contribution.monthYear)
                                    } catch (e: Exception) {
                                        null
                                    }

                                    if (
                                        contribution.paidStatus == PaidStatus.NOT_PAID &&
                                        dueDate != null &&
                                        dueDate.before(Date())
                                    ) {
                                        synchronized(dueContributions) {
                                            dueContributions.add(contribution)
                                        }
                                    }
                                }
                            }

                            latch.countDown()
                        }
                        .addOnFailureListener {
                            latch.countDown()
                        }

                    // ---------------- Loans ----------------

                    loansRef.get()
                        .addOnSuccessListener { loanSnap ->

                            val currentCal = Calendar.getInstance()
                            val currentKey =
                                currentCal.get(Calendar.YEAR) * 12 +
                                        currentCal.get(Calendar.MONTH)

                            for (loanDoc in loanSnap.documents) {

                                loanDoc.toObject(MemberLoan::class.java)?.let { loan ->

                                    for (installment in loan.installments) {

                                        val dueDate = installment.dueDate?.toDate()

                                        if (
                                            installment.status == EMIStatus.PENDING &&
                                            dueDate != null
                                        ) {

                                            val dueCal = Calendar.getInstance().apply {
                                                time = dueDate
                                            }

                                            val dueKey =
                                                dueCal.get(Calendar.YEAR) * 12 +
                                                        dueCal.get(Calendar.MONTH)

                                            if (dueKey <= currentKey) {
                                                synchronized(dueInstallments) {
                                                    dueInstallments.add(installment)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            latch.countDown()
                        }
                        .addOnFailureListener {
                            latch.countDown()
                        }
                }

                Thread {
                    latch.await()

                    Handler(Looper.getMainLooper()).post {
                        completion(
                            dueContributions.toList(),
                            dueInstallments.toList()
                        )
                    }
                }.start()
            }
            .addOnFailureListener {
                println("❌ Error fetching members: ${it.localizedMessage}")
                completion(emptyList(), emptyList())
            }
    }

    fun handleCashFreeResponse(sessionId: String?, orderId: String?, error: Exception? , completion: () -> Unit) {
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (orderId.isNullOrEmpty()) {
            Log.d("Payment", "❌ Missing or empty sessionId/orderId in response")
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "Payment entry could not be created. Please try again.",
                type = AlertType.ERROR,
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        Log.d("Payment", "✅ Payment entry ready | orderId: $orderId, sessionId: $sessionId")

        setShowPayment(true)
        setPaymentOrderId(orderId)
        completion()
    }

    //Payment Changes
    fun retryPayoutAction(payment: PaymentsDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            if (payment.payoutStatus == PayoutStatus.PENDING || payment.payoutStatus == PayoutStatus.PAYOUT_FAILED) {

                LoaderManager.shared.showLoader()
                val isManager = UserDefaultsManager.getSquadManagerLogged()

                val beneId = if (isManager)
                    squad.value?.upiBeneId ?: ""
                else
                    currentMember.value?.upiBeneId ?: ""

                FirebaseFunctionsManager.shared.makeCashFreePayout(
                    squadId = squad.value?.squadID ?: "",
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
                        val index = _squadPayments.value.indexOfFirst { it.id == payment.id }
                        if (index != -1) {
                            val updatedPayment = _squadPayments.value[index].copy(
                                payoutStatus = when (status) {
                                    "SUCCESS" -> PayoutStatus.PAYOUT_SUCCESS
                                    "FAILED" -> PayoutStatus.PAYOUT_FAILED
                                    "RECEIVED", "PENDING", "" -> PayoutStatus.PAYOUT_INPROGRESS
                                    else -> PayoutStatus.PENDING
                                },
                                payoutSuccess = status == "SUCCESS",
                                transferReferenceId = transferId ?: "",
                                paymentUpdatedDate = updatedTimestamp,
                                payoutResponseMessage = data.statusDescription ?: ""
                            )

                            val updatedList = _squadPayments.value.toMutableList()
                            updatedList[index] = updatedPayment
                            setSquadPayments(updatedList)

                            AlertManager.shared.showAlert(
                                title = SquadStrings.appName,
                                message = updatedPayment.payoutResponseMessage,
                                type = if (updatedPayment.payoutStatus == PayoutStatus.PAYOUT_FAILED)
                                    AlertType.ERROR else AlertType.SUCCESS,
                                primaryButtonTitle = SquadStrings.ok
                            )
                        }
                    }

                    result.onFailure { error ->
                        val message = error.message ?: "Failed to process payout. Please try again."
                        AlertManager.shared.showAlert(
                            title = SquadStrings.appName,
                            message = message,
                            type = AlertType.ERROR,
                            primaryButtonTitle = SquadStrings.ok
                        )
                    }
                }

            } else {
                // ✅ Verify existing payout instead of retrying
                LoaderManager.shared.showLoader()
                FirebaseFunctionsManager.shared.verifyCashFreePayoutStatus(
                    squadId = squad.value?.squadID ?: "",
                    paymentId = payment.id ?: "",
                    transferId = payment.transferReferenceId
                ) { result ->
                    LoaderManager.shared.hideLoader()

                    result.onSuccess { data ->
                        val status = data.status?.uppercase()

                        val updatedPayment = payment.copy(
                            payoutStatus = when (status) {
                                "SUCCESS" -> PayoutStatus.PAYOUT_SUCCESS
                                "FAILED" -> PayoutStatus.PAYOUT_FAILED
                                "PENDING", "IN_PROGRESS" -> PayoutStatus.PAYOUT_INPROGRESS
                                else -> PayoutStatus.PENDING
                            },
                            payoutSuccess = status == "SUCCESS",
                            payoutResponseMessage = data.statusDescription ?: "",
                            paymentUpdatedDate = CommonFunctions.parseISODateToTimestamp(data.updatedOn)
                        )

                        val updatedList = _squadPayments.value.toMutableList()
                        val index = updatedList.indexOfFirst { it.id == payment.id }
                        if (index != -1) {
                            updatedList[index] = updatedPayment
                            setSquadPayments(updatedList)
                        }

                        if (updatedPayment.payoutStatus == PayoutStatus.PAYOUT_SUCCESS) {
                            AlertManager.shared.showAlert(
                                title = SquadStrings.appName,
                                message = updatedPayment.payoutResponseMessage,
                                type = AlertType.SUCCESS,
                                primaryButtonTitle = SquadStrings.ok
                            )
                        }
                    }

                    result.onFailure { error ->
                        val message = error.message ?: "Failed to verify payout status. Please try again."
                        AlertManager.shared.showAlert(
                            title = SquadStrings.appName,
                            message = message,
                            type = AlertType.ERROR,
                            primaryButtonTitle = SquadStrings.ok
                        )
                    }
                }
            }
        }
    }

    fun retryPaymentAction(payment: PaymentsDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            if (payment.paymentStatus == PaymentStatus.PENDING || payment.paymentStatus == PaymentStatus.FAILED) {
                LoaderManager.shared.showLoader()

                FirebaseFunctionsManager.shared.processRazorPayPayment(
                    squadId = payment.squadId,
                    action = RazorpayPaymentAction.Retry(failedOrderId = payment.id!!)
                ) { sessionId, orderId, error ->
                    LoaderManager.shared.hideLoader()
                    handleCashFreeResponse(sessionId, orderId, error, completion = {})
                }
            }
        }
    }


    fun fetchManagerLogins(
        showLoader: Boolean,
        phoneNumber: String,
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
            completion(false, SquadStrings.networkError)
            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.fetchManagerLogins(phoneNumber) { loginList, error ->

            // Run on main thread (like DispatchQueue.main.async)
            Handler(Looper.getMainLooper()).post {

                if (showLoader) {
                    LoaderManager.shared.hideLoader()
                }

                if (loginList != null) {
                    setManagerLogins(loginList)
                    completion(true, null)
                } else {
                    val errorMessage = error ?: "No managed squads found"
                    completion(false, errorMessage)
                }
            }
        }
    }

    fun updateMemberLoginStatusForSquad(
        showLoader: Boolean,
        phoneNumber: String,
        squadID: String,
        status: String,
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

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        val members = squadMembers.value
        val latch = CountDownLatch(members.size)

        var hasError = false
        var errorMessage: String? = null

        members.forEach { member ->

            manager.updateMemberLoginStatus(
                phoneNumber = member.phoneNumber,
                squadID = squadID,
                recordStatus = status
            ) { success, error ->

                if (!success) {
                    hasError = true
                    errorMessage = error
                }

                latch.countDown()
            }
        }

        // Wait for all async tasks
        Thread {

            latch.await()

            val db = FirebaseFirestore.getInstance()

            db.collection("users")
                .document(phoneNumber)
                .collection("logins")
                .whereEqualTo("squadID", squadID)
                .whereEqualTo("role", SquadUserType.SQUAD_MANAGER.name)
                .get()
                .addOnSuccessListener { snapshot ->

                    val batch = db.batch()

                    // Update manager login(s)
                    snapshot.documents.forEach { document ->
                        batch.update(
                            document.reference,
                            mapOf(
                                "recordStatus" to status,
                                "recordDate" to Timestamp.now()
                            )
                        )
                    }

                    // Update squad status
                    val squadRef = db.collection("squads").document(squadID)
                    batch.update(
                        squadRef,
                        mapOf(
                            "recordStatus" to status
                        )
                    )

                    batch.commit()
                        .addOnSuccessListener {

                            Handler(Looper.getMainLooper()).post {

                                if (showLoader) {
                                    LoaderManager.shared.hideLoader()
                                }

                                completion(!hasError, errorMessage)
                            }
                        }
                        .addOnFailureListener { e ->

                            Handler(Looper.getMainLooper()).post {

                                if (showLoader) {
                                    LoaderManager.shared.hideLoader()
                                }

                                completion(false, e.localizedMessage ?: "Failed to update squad.")
                            }
                        }
                }
                .addOnFailureListener { e ->

                    Handler(Looper.getMainLooper()).post {

                        if (showLoader) {
                            LoaderManager.shared.hideLoader()
                        }

                        completion(false, e.localizedMessage ?: "Failed to fetch manager login.")
                    }
                }

        }.start()
    }

    fun logoutUser(navController: NavController) {
        UserDefaultsManager.clearAll()

        FirestoreManager.shared.clearFCMTokenForAllUsers(users.value) { success, error ->

            if (success) {
                Log.d("LOGOUT", "✅ FCM tokens cleared")
                FirebaseAuth.getInstance().signOut()
                SessionManager.logins.clear()
            } else {
                Log.e("LOGOUT", "❌ Error: $error")
            }

            navController.navigate(AppDestination.SIGN_IN.route) {
                // Remove the entire back stack
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun fetchCashRequests(
        showLoader: Boolean,
        memberId: String? = null,
        completion: (Boolean, String?) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            completion(false, "No Internet")
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        cashRequestsIsLoadingMore = true

        manager.fetchCashRequests(
            squadID = squadID,
            memberId = memberId,
            lastDocument = null,
            limit = cashRequestsPageSize
        ) { cashRequests, lastDoc, error ->

            cashRequestsIsLoadingMore = false
            LoaderManager.shared.hideLoader()

            if (cashRequests != null) {

                setSquadCashRequests(cashRequests)

                cashRequestsLastDocument = lastDoc

                cashRequestsHasMoreData =
                    cashRequests.size == cashRequestsPageSize

                completion(true, null)

            } else {

                completion(false, error ?: "Failed to fetch cash requests")
            }
        }
    }

    fun resetCashRequestsPagination() {

        setSquadCashRequests(emptyList())

        cashRequestsLastDocument = null

        cashRequestsHasMoreData = true

        cashRequestsIsLoadingMore = false
    }

    fun loadMoreCashRequests(
        memberId: String? = null
    ) {

        if (cashRequestsIsLoadingMore) return
        if (!cashRequestsHasMoreData) return

        val last = cashRequestsLastDocument ?: return

        cashRequestsIsLoadingMore = true

        manager.fetchCashRequests(
            squadID = squadID,
            memberId = memberId,
            lastDocument = last,
            limit = cashRequestsPageSize
        ) { cashRequests, newLastDoc, _ ->

            cashRequestsIsLoadingMore = false

            if (cashRequests != null) {

                val current = squadCashRequests.value.toMutableList()

                current.addAll(cashRequests)

                setSquadCashRequests(current)

                cashRequestsLastDocument = newLastDoc

                cashRequestsHasMoreData =
                    cashRequests.size == cashRequestsPageSize.toInt()
            }
        }
    }

    fun loadMoreCashRequestsIfNeeded(
        currentCashRequest: CashRequest,
        memberId: String? = null
    ) {

        val last = squadCashRequests.value.lastOrNull() ?: return

        if (currentCashRequest.id != last.id) return

        loadMoreCashRequests(memberId)
    }

    fun addCashRequest(
        showLoader: Boolean = true,
        cashRequest: CashRequest,
        completion: (Boolean, String?) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            completion(false, "No Internet")
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.addCashRequest(
            squadID = squadID,
            cashRequest = cashRequest
        ) { documentId, error ->

            LoaderManager.shared.hideLoader()

                val current = squadCashRequests.value.toMutableList()
                current.add(0, cashRequest)
                setSquadCashRequests(current)

                completion(true, null)


        }
    }

    fun updateCashRequestStatus(
        showLoader: Boolean = true,
        squadID: String,
        cashRequestId: String,
        status: CashRequestStatus,
        completion: (Boolean, String?) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {

            LoaderManager.shared.hideLoader()

            completion(false, "No Internet")

            return
        }

        if (showLoader) {
            LoaderManager.shared.showLoader()
        }

        manager.updateCashRequestStatus(
            squadID = squadID,
            cashRequestId = cashRequestId,
            status = status
        ) { error ->

            LoaderManager.shared.hideLoader()

            if (error != null) {

                handleFetchError(error) {

                    updateCashRequestStatus(
                        showLoader = showLoader,
                        squadID = squadID,
                        cashRequestId = cashRequestId,
                        status = status,
                        completion = completion
                    )
                }

                completion(false, error)
                return@updateCashRequestStatus
            }

            // Update local list
            val updatedList = squadCashRequests.value.toMutableList()

            val index = updatedList.indexOfFirst {
                it.id == cashRequestId
            }

            if (index != -1) {

                val request = updatedList[index].copy(
                    cashRequestStatus = status,
                    requestAcceptedOn =
                        if (status == CashRequestStatus.ACCEPTED)
                            Timestamp.now()
                        else
                            updatedList[index].requestAcceptedOn
                )

                updatedList[index] = request

                setSquadCashRequests(updatedList)
            }

            completion(true, null)
        }
    }


    fun makeLoanPayment(
        activity: Activity,
        context: Context,
        selectedMember: Member,
        selectedLoan: EMIConfiguration,
        cashRequestId : String,
        showLoader: Boolean = true,
        completion: (Boolean, String?) -> Unit
    ) {

        val newLoan = CommonFunctions.generateMemberLoan(
            emiConfig = selectedLoan,
            memberID = selectedMember.id ?: "",
            memberName = selectedMember.name
        )

        val newPayment = PaymentsDetails(
            id = CommonFunctions.generatePaymentID(
                squadId = squad.value?.squadID ?: ""
            ),
            paymentUpdatedDate = Timestamp.now(),
            memberId = selectedMember.id ?: "",
            memberName = selectedMember.name,
            paymentPhone = selectedMember.phoneNumber,
            paymentEmail = selectedMember.mailID ?: "",
            userType = SquadUserType.SQUAD_MANAGER,
            amount = selectedLoan.loanAmount,
            paymentStatus = PaymentStatus.INVERIFICATION,
            paymentApproveStatus = PaymentApproveStatus.REQUESTED,
            intrestAmount = 0,
            paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
            paymentType = PaymentType.PAYMENT_DEBIT,
            paymentSubType = PaymentSubType.LOAN_AMOUNT,
            description = "Loan disbursement",
            squadId = squad.value?.squadID ?: "",
            order_id = newLoan.id ?: "",
            contributionId = "",
            loanId = newLoan.id ?: "",
            installmentId = "",
            paymentResponseMessage = "Pending member verification.",
            transferReferenceId = "Loan disbursement to ${selectedMember.name}",
            upiID = selectedMember.upiID,
            selectedEMIConfig = selectedLoan,
            cashRequestId = cashRequestId
        )

        savePayments(
            activity = activity,
            context = context,
            showLoader = showLoader,
            squadID = squad.value?.squadID ?: "",
            payment = listOf(newPayment)
        ) { success, error ->

            completion(success, error)
        }
    }
}