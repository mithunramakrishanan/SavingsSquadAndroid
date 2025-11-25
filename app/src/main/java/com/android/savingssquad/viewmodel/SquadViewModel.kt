package com.android.savingssquad.viewmodel

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
import com.android.savingssquad.singleton.CashfreePaymentAction
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.SquadUserType
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
    private var squadID: String = ""
    var loginMember: Login? = null
    private var squadListener: ListenerRegistration? = null

    // ------------------------------------------------------------------------
    // 🔹 Squad
    // ------------------------------------------------------------------------
    private val _squad = MutableStateFlow<Squad?>(null)
    val squad: StateFlow<Squad?> = _squad
    fun setSquad(value: Squad?) { _squad.value = value }

    private val _rules = MutableStateFlow<List<SquadRule>>(emptyList())
    val rules: StateFlow<List<SquadRule>> = _rules
    fun setRules(list: List<SquadRule>) { _rules.value = list }

    private val _squadActivities = MutableStateFlow<List<SquadActivity>>(emptyList())
    val squadActivities: StateFlow<List<SquadActivity>> = _squadActivities
    fun setSquadActivities(list: List<SquadActivity>) { _squadActivities.value = list }

    private val _squadPayments = MutableStateFlow<List<PaymentsDetails>>(emptyList())
    val squadPayments: StateFlow<List<PaymentsDetails>> = _squadPayments
    fun setSquadPayments(list: List<PaymentsDetails>) { _squadPayments.value = list }

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

    private val _paymentOrderToken = MutableStateFlow("")
    val paymentOrderToken: StateFlow<String> = _paymentOrderToken
    fun setPaymentOrderToken(value: String) { _paymentOrderToken.value = value }

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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                        manager.fetchPayments(squadID) { fetchedPayments, err ->
                            viewModelScope.launch(Dispatchers.IO) {
                                if (fetchedPayments != null) {
                                    _squadPayments.value = fetchedPayments
                                    _isFetchingTotalAmountCollected.value = false
                                } else {
                                    val errorMsg = err ?: "❌ Failed to fetch payments"
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
                primaryButtonTitle = "OK",
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
            upiID = ""
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
            squadID: String,
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
                    primaryButtonTitle = "OK",
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
                    primaryButtonTitle = "OK",
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
                        amount = 0,
                        description = "Added a new member ${member.name} to the squad"
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
                    primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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

    fun fetchSquadActivities(showLoader: Boolean = true, squadID: String) {
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

        manager.fetchSquadActivities(squadID) { activities, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (activities != null) {
                    _squadActivities.value = activities
                } else {
                    val errorMsg = error ?: "Unknown error"
                    println("❌ Error fetching squad activities: $errorMsg")
                    handleFetchError(errorMsg) {
                        fetchSquadActivities(showLoader, squadID)
                    }
                }
            }
        }
    }

    fun deleteSquadActivity(showLoader: Boolean = true, activityID: String) {
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
            return
        }

        if (showLoader) LoaderManager.shared.showLoader()

        manager.savePayments(squadID, payment) { success, error ->
            if (showLoader) LoaderManager.shared.hideLoader()

            if (!success) {
                val errorMsg = error ?: "❌ Failed to add payment"
                println(errorMsg)
                handleFetchError(errorMsg) {
                    savePayments(showLoader, squadID, payment, completion)
                }
                completion(false, errorMsg)
                return@savePayments
            }

            _squadPayments.value = _squadPayments.value.toMutableList().apply {
                addAll(payment)
            }

            val squadLocal = _squad.value
            if (squadLocal == null) {
                completion(false, "❌ Squad not found")
                return@savePayments
            }

            val userId = payment.firstOrNull()?.memberId
            val member = _squadMembers.value.firstOrNull { it.id == userId }
            if (member == null) {
                completion(false, "❌ Member not found")
                return@savePayments
            }

            var squadCopy = squadLocal
            var memberCopy = member
            applyPaymentSummaries(payment, squadCopy, memberCopy)

            CoroutineScope(Dispatchers.IO).launch {
                updateMembers(squadID = squadCopy.squadID, members = listOf(memberCopy)) { memberSuccess, memberError ->
                    if (!memberSuccess) {
                        println("❌ Failed to update member: ${memberError ?: "Unknown error"}")
                    }
                }

                updateSquad(squad = squadCopy) { squadSuccess, _, squadError ->
                    if (!squadSuccess) {
                        println("❌ Failed to update squad: ${squadError ?: "Unknown error"}")
                    }
                }
            }

            completion(true, null)
        }
    }

    fun applyPaymentSummaries(payments: List<PaymentsDetails>, squad: Squad, member: Member) {
        for (pay in payments) {
            when (pay.paymentType) {
                PaymentType.PAYMENT_CREDIT -> {
                    when (pay.paymentSubType) {
                        PaymentSubType.INTEREST_AMOUNT -> {
                            squad.totalInterestAmountReceived += pay.intrestAmount
                            member.totalInterestPaid += pay.intrestAmount
                        }

                        PaymentSubType.EMI_AMOUNT -> {
                            squad.totalLoanAmountReceived += (pay.amount - pay.intrestAmount)
                            member.totalLoanPaid += (pay.amount - pay.intrestAmount)
                        }

                        PaymentSubType.CONTRIBUTION_AMOUNT -> {
                            squad.totalContributionAmountReceived += pay.amount
                            member.totalContributionPaid += pay.amount
                        }

                        PaymentSubType.OTHERS_AMOUNT -> Unit
                        else -> Unit
                    }
                    squad.currentAvailableAmount += pay.amount
                }

                PaymentType.PAYMENT_DEBIT -> {
                    if (pay.paymentSubType == PaymentSubType.LOAN_AMOUNT) {
                        squad.totalLoanAmountSent += (pay.amount - pay.intrestAmount)
                        member.totalLoanBorrowed += (pay.amount - pay.intrestAmount)
                        squad.currentAvailableAmount -= pay.amount
                    }
                }
            }
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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

        manager.fetchPayments(squadID) { fetchedPayments, error ->
            MainScope().launch {
                if (showLoader) LoaderManager.shared.hideLoader()

                if (fetchedPayments != null) {
                    _squadPayments.value = fetchedPayments.toMutableList()
                    completion(true, null)
                } else {
                    val errorMsg = error ?: "❌ Failed to fetch payments"
                    println(errorMsg)
//                    handleFetchError(errorMsg) {
//                        fetchPayments(showLoader, completion)
//                    }
                    completion(false, errorMsg)
                }
            }
        }
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
                primaryButtonTitle = "OK",
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
            primaryButtonTitle = "OK",
            primaryAction = {},
            secondaryButtonTitle = "Retry",
            secondaryAction = retryAction
        )
    }

    fun createSquadActivity(
        activityType: SquadActivityType,
        userName: String,
        amount: Int,
        description: String,
        alertOK: (() -> Unit)? = null
    ) {
        val squad = _squad.value ?: run {
            println("❌ No squad found!")
            return
        }

        val activity = SquadActivity(
            squadID = squad.squadID,
            squadName = squad.squadName,
            date = Timestamp.now(),
            activityType = activityType,
            userName = userName,
            amount = amount,
            description = description
        )

        if (UserDefaultsManager.getLogin()?.squadID != null) {
            addSquadActivity(true, activity) { success, error ->
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
                        createSquadActivity(activityType, userName, amount, description, alertOK)
                    }
                }
            }
        } else {
            println("❌ No squad found!")
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
        viewModelScope.launch(Dispatchers.IO) {
            if (payment.paymentStatus == PaymentStatus.PENDING || payment.paymentStatus == PaymentStatus.FAILED) {
                LoaderManager.shared.showLoader()

                FirebaseFunctionsManager.shared.processCashFreePayment(
                    squadId = payment.squadId,
                    action = CashfreePaymentAction.Retry(failedOrderId = payment.order_id)
                ) { sessionId, orderId, error ->
                    LoaderManager.shared.hideLoader()
                    handleCashFreeResponse(sessionId, orderId, error, completion = {})
                }
            }
        }
    }
}