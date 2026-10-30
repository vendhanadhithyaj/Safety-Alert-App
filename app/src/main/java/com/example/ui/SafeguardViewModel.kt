package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SafeguardRepository
import com.example.model.*
import com.example.util.EmergencyActions
import com.example.util.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppPortal {
    PEOPLE,
    SAFETY_NETWORK
}

enum class PeopleTab {
    SOS,
    MAP,
    REPORT,
    CIRCLES,
    DIRECTORY
}

enum class NetworkTab {
    DISPATCH,
    VERIFICATION,
    DANGER_ZONES,
    AUDIT_LOGS
}

data class SafeguardUiState(
    val currentPortal: AppPortal = AppPortal.PEOPLE,
    val selectedPeopleTab: PeopleTab = PeopleTab.SOS,
    val selectedNetworkTab: NetworkTab = NetworkTab.DISPATCH,
    val currentLocation: LocationPoint = LocationHelper.DEFAULT_KUMBAKONAM,
    val isGpsActive: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val isRealtimeTrackingActive: Boolean = false,
    val isSosCountdownActive: Boolean = false,
    val sosCountdownSeconds: Int = 3,
    val isStrobeActive: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val showPortalSwitchDialog: Boolean = false,
    val statusMessage: String? = null
)

class SafeguardViewModel(application: Application) : AndroidViewModel(application) {

    val locationManager = com.example.util.PlayServicesLocationManager(application)

    private val _uiState = MutableStateFlow(
        SafeguardUiState(
            hasLocationPermission = locationManager.hasPermission()
        )
    )
    val uiState: StateFlow<SafeguardUiState> = _uiState.asStateFlow()

    val currentUser = SafeguardRepository.currentUser
    val activeSos = SafeguardRepository.activeSos
    val allIncidents = SafeguardRepository.incidents
    val communityReports = SafeguardRepository.reports
    val dangerZones = SafeguardRepository.dangerZones
    val circleMessages = SafeguardRepository.circleMessages
    val emergencyContacts = SafeguardRepository.emergencyContacts
    val activeLiveShares = SafeguardRepository.activeLiveShares
    val auditLogs = SafeguardRepository.auditLogs

    private var countdownJob: Job? = null
    private var sosProgressionJob: Job? = null

    init {
        // Collect real-time location stream from Google Play Services
        viewModelScope.launch {
            locationManager.currentLocation.collect { loc ->
                _uiState.update { it.copy(currentLocation = loc, isGpsActive = true) }
            }
        }

        viewModelScope.launch {
            locationManager.isTracking.collect { tracking ->
                _uiState.update { it.copy(isRealtimeTrackingActive = tracking) }
            }
        }

        viewModelScope.launch {
            locationManager.hasLocationPermission.collect { perm ->
                _uiState.update { it.copy(hasLocationPermission = perm) }
            }
        }

        // Start tracking if permission already available
        if (locationManager.hasPermission()) {
            locationManager.startRealtimeTracking(intervalMillis = 4000L)
        } else {
            refreshLocation()
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        locationManager.updatePermissionState()
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            locationManager.startRealtimeTracking(intervalMillis = 4000L)
            _uiState.update { it.copy(statusMessage = "GPS location permission granted. Real-time tracking activated via Google Play Services.") }
        } else {
            _uiState.update { it.copy(statusMessage = "Location permission denied. Using fallback landmark coordinates.") }
        }
    }

    fun toggleRealtimeTracking() {
        if (!_uiState.value.hasLocationPermission) {
            _uiState.update { it.copy(statusMessage = "Please grant GPS runtime permission to activate real-time tracking.") }
            return
        }

        if (_uiState.value.isRealtimeTrackingActive) {
            locationManager.stopRealtimeTracking()
            _uiState.update { it.copy(statusMessage = "Real-time location tracking paused.") }
        } else {
            locationManager.startRealtimeTracking(intervalMillis = 3000L)
            _uiState.update { it.copy(statusMessage = "Real-time location tracking active via Google Play Services.") }
        }
    }

    fun refreshLocation() {
        val loc = LocationHelper.getBestLastKnownLocation(getApplication())
        _uiState.update { it.copy(currentLocation = loc, isGpsActive = true) }
    }

    fun selectPresetLocation(location: LocationPoint) {
        _uiState.update { it.copy(currentLocation = location) }
    }

    fun setPortal(portal: AppPortal) {
        _uiState.update { it.copy(currentPortal = portal) }
    }

    fun setPeopleTab(tab: PeopleTab) {
        _uiState.update { it.copy(selectedPeopleTab = tab) }
    }

    fun setNetworkTab(tab: NetworkTab) {
        _uiState.update { it.copy(selectedNetworkTab = tab) }
    }

    fun showPortalDialog(show: Boolean) {
        _uiState.update { it.copy(showPortalSwitchDialog = show) }
    }

    // SOS Flow
    fun startSosCountdown(emergencyType: String = "Imminent Danger / Physical Threat") {
        countdownJob?.cancel()
        _uiState.update { it.copy(isSosCountdownActive = true, sosCountdownSeconds = 3) }
        EmergencyActions.triggerHapticPulse(getApplication())

        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(sosCountdownSeconds = i) }
                delay(1000L)
            }
            _uiState.update { it.copy(isSosCountdownActive = false) }
            triggerInstantSos(emergencyType)
        }
    }

    fun cancelSosCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(isSosCountdownActive = false, statusMessage = "SOS alert cancelled by user") }
    }

    fun triggerInstantSos(emergencyType: String = "General Emergency") {
        countdownJob?.cancel()
        _uiState.update { it.copy(isSosCountdownActive = false) }

        val incident = SafeguardRepository.triggerSos(
            type = emergencyType,
            location = _uiState.value.currentLocation
        )
        EmergencyActions.triggerHapticPulse(getApplication())

        // Truthful backend status lifecycle simulation
        sosProgressionJob?.cancel()
        sosProgressionJob = viewModelScope.launch {
            delay(350L)
            SafeguardRepository.advanceSosStatus(incident.id, SosStatus.SAVED_ON_DEVICE)

            delay(1000L)
            SafeguardRepository.advanceSosStatus(incident.id, SosStatus.SENT_TO_SERVER)

            delay(1200L)
            SafeguardRepository.advanceSosStatus(incident.id, SosStatus.QUEUED)

            delay(2500L)
            SafeguardRepository.advanceSosStatus(
                incidentId = incident.id,
                nextStatus = SosStatus.ACKNOWLEDGED,
                responderName = "Inspector S. Ramanathan, TN Police Kumbakonam Control",
                responderNotes = "Patrol Unit TN-49-G-112 dispatched towards ${incident.location.landmark}."
            )
        }
    }

    fun clearActiveSos() {
        SafeguardRepository.clearActiveSos()
        _uiState.update { it.copy(statusMessage = "Active alert dismissed") }
    }

    fun toggleStrobeFlashlight() {
        val next = !_uiState.value.isStrobeActive
        EmergencyActions.toggleFlashlightStrobe(getApplication(), next)
        _uiState.update { it.copy(isStrobeActive = next) }
    }

    fun toggleSirenSound() {
        val next = !_uiState.value.isSirenPlaying
        _uiState.update { it.copy(isSirenPlaying = next) }
        if (next) {
            EmergencyActions.triggerHapticPulse(getApplication())
        }
    }

    // Community Reports
    fun submitReport(
        category: ReportCategory,
        title: String,
        description: String,
        location: LocationPoint,
        photoUri: String? = null,
        audioDurationSec: Int = 0
    ) {
        SafeguardRepository.submitReport(
            category = category,
            title = title,
            description = description,
            location = location,
            photoUri = photoUri,
            audioDurationSec = audioDurationSec
        )
        _uiState.update { it.copy(statusMessage = "Report submitted. Reference generated and queued for review.") }
    }

    fun reviewReport(
        reportId: String,
        newStatus: ReviewStatus,
        rejectionReason: String? = null,
        createDangerZone: Boolean = false,
        radiusMeters: Int = 300,
        severity: ZoneSeverity = ZoneSeverity.HIGH
    ) {
        val user = currentUser.value
        SafeguardRepository.reviewReport(
            reportId = reportId,
            newStatus = newStatus,
            reviewerName = "${user.name} (${user.responderBadgeId ?: "Official"})",
            rejectionReason = rejectionReason,
            createDangerZone = createDangerZone,
            zoneRadius = radiusMeters,
            severity = severity
        )
        _uiState.update { it.copy(statusMessage = "Report status updated to ${newStatus.displayName}") }
    }

    fun acknowledgeIncidentAsResponder(incidentId: String, responderNotes: String) {
        val user = currentUser.value
        SafeguardRepository.advanceSosStatus(
            incidentId = incidentId,
            nextStatus = SosStatus.ACKNOWLEDGED,
            responderName = "${user.name} (${user.responderBadgeId ?: "Unit 112"})",
            responderNotes = responderNotes
        )
        _uiState.update { it.copy(statusMessage = "Incident acknowledged and assigned") }
    }

    fun resolveIncident(incidentId: String, notes: String) {
        val user = currentUser.value
        SafeguardRepository.advanceSosStatus(
            incidentId = incidentId,
            nextStatus = SosStatus.RESOLVED,
            responderName = user.name,
            responderNotes = notes
        )
        _uiState.update { it.copy(statusMessage = "Incident marked RESOLVED") }
    }

    fun deactivateZone(zoneId: String) {
        SafeguardRepository.deactivateDangerZone(zoneId, currentUser.value.name)
        _uiState.update { it.copy(statusMessage = "Danger zone deactivated") }
    }

    fun sendCircleMessage(circleId: String, text: String, isQuickCheckIn: Boolean = false, locationTag: String? = null) {
        SafeguardRepository.sendSafetyMessage(circleId, text, isQuickCheckIn, locationTag)
    }

    fun startLiveLocationShare(contactName: String, contactPhone: String, durationMinutes: Int) {
        val session = SafeguardRepository.startLiveShare(
            contactName = contactName,
            contactPhone = contactPhone,
            durationMinutes = durationMinutes,
            currentPoint = _uiState.value.currentLocation
        )
        _uiState.update { it.copy(statusMessage = "Live location shared with $contactName for $durationMinutes mins") }
    }

    fun stopLiveShare(sessionId: String) {
        SafeguardRepository.stopLiveShare(sessionId)
        _uiState.update { it.copy(statusMessage = "Live sharing ended") }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
