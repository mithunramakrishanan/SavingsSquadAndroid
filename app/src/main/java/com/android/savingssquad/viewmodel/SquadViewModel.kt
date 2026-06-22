package com.android.savingssquad.viewmodel

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.android.savingssquad.singleton.PayoutStatus
import com.android.savingssquad.model.pendingInstallments
import com.android.savingssquad.model.pendingLoans
import com.android.savingssquad.singleton.AlertType
import com.android.savingssquad.singleton.AmountEditType
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentApproveStatus
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
import com.android.savingssquad.singleton.UPIPaymentManager
import com.android.savingssquad.singleton.UPIPaymentStatus
import com.google.firebase.firestore.*
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
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
    private var squadListener: ListenerRegistration? = null

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

    init {
        val login = UserDefaultsManager.getLogin()
        if (login != null) {
            squadID = login.squadID.toString()
            loginMember = login
            _selectedUser.value = login
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
            _selectedUser.value = defaultLogin
        }

        observeSquadChanges()
    }

    override fun onCleared() {
        super.onCleared()
        squadListener?.remove()
    }

    private fun observeSquadChanges() {
        if (squadID.isEmpty()) return

        squadListener?.remove()
        val db = FirebaseFirestore.getInstance()

        squadListener = db.collection("squads")
            .document(squadID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("❌ Failed to observe squad changes: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    println("⚠️ Squad document deleted or doesn't exist")
                    return@addSnapshotListener
                }
                println("🔄 Squad updated remotely, refetching data...")
                fetchSquadByID(showLoader = false) { success, _, _ ->
                    println(if (success) "✅ Squad re-fetched on update" else "❌ Re-fetch failed")
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
                        if (loginList.size > 1) {
                            multipleAccount = true
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
            _selectedUser.value = login
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

                    _squad.value = fetchedSquad
                    _remainingMonths.value = CommonFunctions.getRemainingMonths(
                        startDate = Date(),
                        endDate = fetchedSquad.squadEndDate?.toDate() ?: Date()
                    )

                    _isFetchingTotalAmountCollected.value = true

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
                                    _squadPayments.value = fetchedPayments
                                    _isFetchingTotalAmountCollected.value = false
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
                    _squad.value = updatedSquad
                    _remainingMonths.value = CommonFunctions.getRemainingMonths(
                        startDate = Date(),
                        endDate = updatedSquad.squadEndDate?.orNow ?: Date()
                    )
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
                    _squad.value = updatedSquad
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
            id = CommonFunctions.generateMemberID(),
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
            fcmToken = ""
        )

        val addAndSave: () -> Unit = {
            _showAddMemberPopup.value = false
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
                            _currentMember.value = fetchedMember
                            completion(true, fetchedMember, null)
                        } else {
                            val errorMsg = error ?: "❌ Failed to fetch member"
                            _errorMessage.value = errorMsg

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
                        _isFetchingMembers.value = false
                        if (showLoader) LoaderManager.shared.hideLoader()

                        when {
                            error == "No members found." -> {
                                _squadMembersCount.value = 0
                                completion(false, null, error)
                            }

                            fetchedMembers != null -> {
                                // ✅ Reset & update member list
                                _squadMembers.value = fetchedMembers
                                _squadMembersCount.value = fetchedMembers.size
                                _squadMemberNames.value = fetchedMembers.map { it.name }.toMutableList()

                                // ✅ Auto-select login member (if applicable)
                                loginMember?.squadUsername?.let { username ->
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
            manager.addSquadActivity(squadID, activity) { success, error ->
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

                    _squadActivities.value = activities

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

        _squadActivities.value = emptyList()

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

                    _squadActivities.value = updated

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
                    _squadActivities.value = _squadActivities.value.filter { it.squadID != activityID }
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
                    _rules.value = rules
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
                    _rules.value = _rules.value.filter { it.id != ruleID }
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
        squadID: String,
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.updateContributionStatus(squadID, memberID, contributionID, newStatus) { success, message ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (!success) {
                val errorMsg = message ?: "❌ Failed to update contribution status"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    updateContributionStatus(showLoader, squadID, memberID, contributionID, newStatus, completion)
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

                _squadPayments.value = _squadPayments.value.toMutableList().apply {
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

        manager.savePayments(squadID, payment) { success, error ->
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

            _squadPayments.value = _squadPayments.value.toMutableList().apply {
                addAll(payment)
            }


            for (payment in payment) {

                if (payment.paymentSubType == PaymentSubType.CONTRIBUTION_AMOUNT) {

                    updateContributionStatus(
                        squadID = payment.squadId,
                        memberID = payment.memberId,
                        contributionID = payment.contributionId,
                        newStatus = PaidStatus.INVERIFICATION.value
                    ) { success, error ->

                        // handle response if needed
                        if (!success) {
                            println("Error updating: $error")
                        }
                    }
                }
                else if (payment.paymentSubType == PaymentSubType.EMI_AMOUNT) {

                    updateInstallmentStatus(squadID = payment.squadId, memberID = payment.memberId, loanID = payment.loanId, installmentID = payment.installmentId, status = EMIStatus.INVERIFICATION.value){ success, error ->
                        // handle response if needed
                        if (!success) {
                            println("Error updating: $error")
                        }
                        else {

                            if (_isPendingLoanAvailable.value) {
                                updateLoanPaidAfterInstallmentSettled(_memberPendingLoans.value ?: emptyList(), payment.memberId)
                            }
                        }
                    }
                }

                if (payment.paymentSubType == PaymentSubType.OTHERS_AMOUNT) {
                    updatePaymentCalculations(listOf(payment), PaymentApproveStatus.ACCEPTED)
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
            }

            // 🔹 Update member if exists
            if (status == PaymentApproveStatus.ACCEPTED && member != null) {
                var memberCopy = member
                applyMemberSummaries(payment, memberCopy)
                updateMembers(squadID = squadLocal.squadID, members = listOf(memberCopy)) { success, error ->
                    if (!success) println("❌ Failed to update member: ${error ?: "Unknown error"}")
                }
            }
        }
    }

    fun applyMemberSummaries(payments: List<PaymentsDetails>, member: Member) {
        for (pay in payments) {
            when (pay.paymentType) {
                PaymentType.PAYMENT_CREDIT -> {
                    when (pay.paymentSubType) {
                        PaymentSubType.INTEREST_AMOUNT -> {
                            member.totalInterestPaid += pay.intrestAmount
                        }
                        PaymentSubType.EMI_AMOUNT -> {
                            member.totalLoanPaid += (pay.amount - pay.intrestAmount)
                            member.totalInterestPaid += pay.intrestAmount
                        }
                        PaymentSubType.CONTRIBUTION_AMOUNT -> {
                            member.totalContributionPaid += pay.amount
                        }
                        else -> Unit
                    }
                }
                PaymentType.PAYMENT_DEBIT -> {
                    if (pay.paymentSubType == PaymentSubType.LOAN_AMOUNT) {
                        member.totalLoanBorrowed += (pay.amount - pay.intrestAmount)
                    }
                }
            }
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
                        updates["totalLoanAmountReceived"] = FieldValue.increment((payment.amount - payment.intrestAmount).toLong())
                        updates["totalInterestAmountReceived"] = FieldValue.increment(payment.intrestAmount.toLong())
                        updates["currentCreditAmount"] = FieldValue.increment((payment.amount - payment.intrestAmount).toLong())
                        updates["currentAvailableAmount"] = FieldValue.increment(payment.amount.toLong())
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

                _squadPayments.value = list;

                // remove from pending list
                val pending = pendingApprovalPayments.value.toMutableList() ?: mutableListOf()

                pending.removeAll { it.id == paymentID }

                _pendingApprovalPayments.value = pending;

                updatePaymentCalculations(listOf(payment), status)

                // contribution logic
                if (payment.paymentSubType == PaymentSubType.CONTRIBUTION_AMOUNT) {

                    when (status) {

                        PaymentApproveStatus.ACCEPTED -> {
                            updateContributionStatus(
                                squadID = payment.squadId,
                                memberID = payment.memberId,
                                contributionID = payment.contributionId,
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

                            updateLoanAndAllInstallmentsStatus(
                                squadID = payment.squadId,
                                memberID = payment.memberId,
                                loanID = payment.loanId,
                                status = EMIStatus.PENDING.value
                            ) { success, message ->

                                if (success) {
                                    // ✅ success handling
                                } else {
                                    // ❌ error handling
                                    Log.e("LoanUpdate", message ?: "Unknown error")
                                }
                            }
                        }

                        PaymentApproveStatus.REJECTED -> {

                            updateLoanAndAllInstallmentsStatus(
                                squadID = payment.squadId,
                                memberID = payment.memberId,
                                loanID = payment.loanId,
                                status = EMIStatus.FAILED.value
                            ) { success, message ->

                                if (success) {
                                    // ✅ success handling
                                } else {
                                    // ❌ error handling
                                    Log.e("LoanUpdate", message ?: "Unknown error")
                                }
                            }
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
                            _squadMembers.value = currentList
                        }
                    }

                    _squadMembersCount.value = _squadMembers.value.size
                    _squadMemberNames.value = _squadMembers.value.map { it.name }

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

        manager.updateMemberMobileNumber(squadID, memberID, mobileNumber) { success, error ->
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
            limit = paymentsPageSize
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

    fun observePayments() {
        manager.observePayments(squadID) { updatedPayments, error ->
            if (updatedPayments != null) {
                _squadPayments.value = updatedPayments
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
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        _isPendingLoanAvailable.value = false

        manager.fetchMemberLoans(squadID, memberID) { loans, error ->
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

        membersRef.get().addOnSuccessListener { snapshot ->
            val members = snapshot.documents
            val latch = CountDownLatch(members.size)

            for (memberDoc in members) {
                val memberID = memberDoc.id
                val contribRef = membersRef.document(memberID).collection("contributions")
                val loansRef = membersRef.document(memberID).collection("loans")

                contribRef.get().addOnSuccessListener { contribSnap ->
                    val formatter = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)
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
        _showPayment.value = true
        _paymentOrderId.value = orderId
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
                            _squadPayments.value = updatedList

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
                            _squadPayments.value = updatedList
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
}