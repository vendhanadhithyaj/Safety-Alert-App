package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object SafeguardRepository {

    private val _currentUser = MutableStateFlow(UserProfile())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _incidents = MutableStateFlow<List<SosIncident>>(emptyList())
    val incidents: StateFlow<List<SosIncident>> = _incidents.asStateFlow()

    private val _activeSos = MutableStateFlow<SosIncident?>(null)
    val activeSos: StateFlow<SosIncident?> = _activeSos.asStateFlow()

    private val _reports = MutableStateFlow<List<CommunityReport>>(emptyList())
    val reports: StateFlow<List<CommunityReport>> = _reports.asStateFlow()

    private val _dangerZones = MutableStateFlow<List<DangerZone>>(emptyList())
    val dangerZones: StateFlow<List<DangerZone>> = _dangerZones.asStateFlow()

    private val _circleMessages = MutableStateFlow<List<SafetyMessage>>(emptyList())
    val circleMessages: StateFlow<List<SafetyMessage>> = _circleMessages.asStateFlow()

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()

    private val _activeLiveShares = MutableStateFlow<List<LiveShareSession>>(emptyList())
    val activeLiveShares: StateFlow<List<LiveShareSession>> = _activeLiveShares.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    init {
        seedInitialData()
    }

    private fun seedInitialData() {
        // Initial verified danger zones around Kumbakonam & Tamil Nadu
        val initialZones = listOf(
            DangerZone(
                id = "DZ-KUM-01",
                title = "Unlit Cauvery Bund Stretch",
                description = "Broken streetlights and isolated path after 7:30 PM. High caution advised for unaccompanied students.",
                category = ReportCategory.UNLIT_ROAD,
                location = LocationPoint(10.9635, 79.3810, "Cauvery North Bund Road, Kumbakonam", "Thanjavur", "Tamil Nadu"),
                radiusMeters = 350,
                severity = ZoneSeverity.HIGH,
                approvedByAuthority = "Kumbakonam Taluk Police & Municipal Council"
            ),
            DangerZone(
                id = "DZ-KUM-02",
                title = "Monsoon Waterlogging Underpass",
                description = "Severe water accumulation near Railway underpass during heavy rain. Risk of vehicle stall.",
                category = ReportCategory.WATERLOGGING,
                location = LocationPoint(10.9678, 79.3895, "Railway Underbridge, Station Road, Kumbakonam", "Thanjavur", "Tamil Nadu"),
                radiusMeters = 200,
                severity = ZoneSeverity.MEDIUM,
                approvedByAuthority = "Tamil Nadu Fire & Rescue Services (Kumbakonam)"
            ),
            DangerZone(
                id = "DZ-TNJ-03",
                title = "Narrow High-Speed Transit Junction",
                description = "Frequent pedestrian-vehicle near-misses during school closing hours (4 PM - 5:30 PM).",
                category = ReportCategory.TRAFFIC_HAZARD,
                location = LocationPoint(10.7890, 79.1390, "Old Bus Stand Crossing, Thanjavur", "Thanjavur", "Tamil Nadu"),
                radiusMeters = 150,
                severity = ZoneSeverity.MEDIUM,
                approvedByAuthority = "Thanjavur Traffic Police Division"
            )
        )
        _dangerZones.value = initialZones

        // Initial verified community reports
        val initialReports = listOf(
            CommunityReport(
                id = "REP-TN-8491",
                reporterId = "rep-user-09",
                reporterName = "Aravindh Swaminathan",
                category = ReportCategory.UNLIT_ROAD,
                title = "5 Streetlights out along Mahamaham Tank North",
                description = "Multiple LED poles flickering and off for the past 3 nights. Road is completely dark by 8 PM.",
                location = LocationPoint(10.9595, 79.3802, "Mahamaham Tank North Bank, Kumbakonam", "Thanjavur", "Tamil Nadu"),
                timestamp = System.currentTimeMillis() - 7200000L,
                status = ReviewStatus.VERIFIED,
                verifiedBy = "Inspector S. Muruganathan (TN Police)",
                verifiedAt = System.currentTimeMillis() - 3600000L,
                isDangerZoneCreated = true
            ),
            CommunityReport(
                id = "REP-TN-8495",
                reporterId = "rep-user-14",
                reporterName = "Meenakshi Sundaram",
                category = ReportCategory.HARASSMENT,
                title = "Group loitering near Girls Higher Secondary bus stop",
                description = "Suspicious group passing remarks near bus stop between 5 PM and 6 PM. Community patrol requested.",
                location = LocationPoint(10.9620, 79.3840, "Near Town High School bus stop, Kumbakonam", "Thanjavur", "Tamil Nadu"),
                timestamp = System.currentTimeMillis() - 1800000L,
                status = ReviewStatus.UNDER_REVIEW,
                verifiedBy = "Sub-Inspector K. Geetha"
            ),
            CommunityReport(
                id = "REP-TN-8502",
                reporterId = "rep-user-22",
                reporterName = "Vignesh Kumar",
                category = ReportCategory.TRAFFIC_HAZARD,
                title = "Open trench on Kumbakonam-Thanjavur Highway",
                description = "Drainage construction work left without caution reflective tape or barricades.",
                location = LocationPoint(10.9450, 79.3650, "Papanasam Main Road bypass", "Thanjavur", "Tamil Nadu"),
                timestamp = System.currentTimeMillis() - 900000L,
                status = ReviewStatus.PENDING_REVIEW
            )
        )
        _reports.value = initialReports

        // Authoritative Emergency Directory (Tamil Nadu and National)
        _emergencyContacts.value = listOf(
            EmergencyContact("National Emergency Helpline", "112", "National", "All-in-one Emergency: Police, Fire, Ambulance, Disaster", true),
            EmergencyContact("Tamil Nadu Police Control Room", "100", "Tamil Nadu Official", "State Police emergency dispatch & assistance", true),
            EmergencyContact("Women Helpline (All India)", "1091", "National", "24/7 dedicated support for women in distress", true),
            EmergencyContact("Childline (National)", "1098", "National", "Emergency care & protection for children & students", true),
            EmergencyContact("Emergency Medical / Ambulance", "108", "Tamil Nadu Official", "Free 24/7 Emergency Medical Ambulance Service", true),
            EmergencyContact("Fire & Rescue Services", "101", "Tamil Nadu Official", "Fire outbreaks, structural collapse, rescue operations", true),
            EmergencyContact("Tamil Nadu Women & Child Safety", "181", "Tamil Nadu Official", "State toll-free helpline for domestic & street safety", true),
            EmergencyContact("Senior Citizens Helpline", "14567", "Tamil Nadu Official", "Elder helpline for emergency care and assistance", true),
            EmergencyContact("Kumbakonam East Police Station", "04352430100", "Kumbakonam Local", "Station Inspector: Kumbakonam Town Division", true),
            EmergencyContact("Kumbakonam All Women Police Station", "04352402100", "Kumbakonam Local", "Dedicated all-women police unit, Kumbakonam", true),
            EmergencyContact("Kumbakonam Taluk Police Station", "04352420100", "Kumbakonam Local", "Rural & Sub-division jurisdiction, Kumbakonam", true),
            EmergencyContact("Kumbakonam Government Hospital (GH)", "04352424050", "Kumbakonam Local", "Government District Headquarters Hospital, Casualty", true)
        )

        // Initial Circle Messages
        _circleMessages.value = listOf(
            SafetyMessage(
                circleId = "circle-family",
                senderName = "Amma (Mother)",
                senderRole = "Parent",
                message = "Kavitha, did your coaching class at Kumbakonam finish? Take the main road.",
                timestamp = System.currentTimeMillis() - 3600000L
            ),
            SafetyMessage(
                circleId = "circle-family",
                senderName = "Kavitha (You)",
                senderRole = "Student",
                message = "Yes Amma, class over. Walking with Priya via Town Hall road.",
                timestamp = System.currentTimeMillis() - 2400000L,
                isQuickCheckIn = true,
                locationTag = "Kumbakonam Town Hall Road"
            ),
            SafetyMessage(
                circleId = "circle-transit",
                senderName = "Sundar (Transit Volunteer)",
                senderRole = "Community Patrol",
                message = "Cauvery bridge path well-lit today. Volunteer team stationed at bus depot.",
                timestamp = System.currentTimeMillis() - 1200000L
            )
        )

        // Seed audit logs
        logAudit("SYSTEM_INIT", "SAFEGUARD Core", "SYSTEM", "Platform initialized with Kumbakonam & Tamil Nadu baseline safety directory.")
    }

    // Role switching & User profile
    fun switchRole(newRole: UserRole, responderBadge: String? = null) {
        val old = _currentUser.value
        _currentUser.value = old.copy(
            role = newRole,
            responderBadgeId = responderBadge ?: if (newRole != UserRole.PUBLIC) "TN-POL-KUM-482" else null
        )
        logAudit("ROLE_SWITCH", _currentUser.value.name, _currentUser.value.id, "Switched role to ${newRole.displayName} (Badge: ${_currentUser.value.responderBadgeId})")
    }

    // SOS Lifecyle
    fun triggerSos(type: String, location: LocationPoint): SosIncident {
        val user = _currentUser.value
        val incident = SosIncident(
            id = "SG-TN-${(1000..9999).random()}",
            userId = user.id,
            userName = user.name,
            userPhone = user.phone,
            emergencyType = type,
            location = location,
            timestamp = System.currentTimeMillis(),
            status = SosStatus.CREATED
        )
        _activeSos.value = incident
        _incidents.value = listOf(incident) + _incidents.value
        logAudit("SOS_TRIGGERED", user.name, incident.id, "Emergency SOS triggered: $type at ${location.landmark}")
        return incident
    }

    fun advanceSosStatus(incidentId: String, nextStatus: SosStatus, responderNotes: String? = null, responderName: String? = null) {
        val current = _incidents.value.map { inc ->
            if (inc.id == incidentId) {
                inc.copy(
                    status = nextStatus,
                    responderNotes = responderNotes ?: inc.responderNotes,
                    acknowledgedBy = responderName ?: inc.acknowledgedBy,
                    resolvedAt = if (nextStatus == SosStatus.RESOLVED) System.currentTimeMillis() else inc.resolvedAt
                )
            } else inc
        }
        _incidents.value = current

        if (_activeSos.value?.id == incidentId) {
            _activeSos.value = current.firstOrNull { it.id == incidentId }
        }

        logAudit("SOS_STATUS_UPDATED", responderName ?: _currentUser.value.name, incidentId, "Status advanced to ${nextStatus.label}")
    }

    fun clearActiveSos() {
        _activeSos.value = null
    }

    // Community Reports
    fun submitReport(
        category: ReportCategory,
        title: String,
        description: String,
        location: LocationPoint,
        photoUri: String? = null,
        audioDurationSec: Int = 0
    ): CommunityReport {
        val user = _currentUser.value
        val report = CommunityReport(
            id = "REP-TN-${(1000..9999).random()}",
            reporterId = user.id,
            reporterName = user.name,
            category = category,
            title = title,
            description = description,
            location = location,
            photoUri = photoUri,
            audioDurationSec = audioDurationSec,
            status = ReviewStatus.PENDING_REVIEW
        )
        _reports.value = listOf(report) + _reports.value
        logAudit("REPORT_SUBMITTED", user.name, report.id, "Submitted community hazard report: $title (${category.displayName})")
        return report
    }

    fun reviewReport(
        reportId: String,
        newStatus: ReviewStatus,
        reviewerName: String,
        rejectionReason: String? = null,
        createDangerZone: Boolean = false,
        zoneRadius: Int = 300,
        severity: ZoneSeverity = ZoneSeverity.HIGH
    ) {
        val currentReports = _reports.value.map { rep ->
            if (rep.id == reportId) {
                rep.copy(
                    status = newStatus,
                    rejectionReason = rejectionReason,
                    verifiedBy = reviewerName,
                    verifiedAt = System.currentTimeMillis(),
                    isDangerZoneCreated = createDangerZone || rep.isDangerZoneCreated
                )
            } else rep
        }
        _reports.value = currentReports

        val targetReport = currentReports.firstOrNull { it.id == reportId }

        if (newStatus == ReviewStatus.VERIFIED && createDangerZone && targetReport != null) {
            val newZone = DangerZone(
                id = "DZ-TN-${(100..999).random()}",
                title = targetReport.title,
                description = targetReport.description,
                category = targetReport.category,
                location = targetReport.location,
                radiusMeters = zoneRadius,
                severity = severity,
                approvedByAuthority = "$reviewerName (Tamil Nadu Safety Network)"
            )
            _dangerZones.value = listOf(newZone) + _dangerZones.value
            logAudit("DANGER_ZONE_ACTIVATED", reviewerName, newZone.id, "Activated Danger Zone for verified report ${targetReport.id}")
        }

        logAudit("REPORT_REVIEWED", reviewerName, reportId, "Review status set to ${newStatus.displayName} (Reason: ${rejectionReason ?: "Approved"})")
    }

    fun deactivateDangerZone(zoneId: String, actor: String) {
        _dangerZones.value = _dangerZones.value.map {
            if (it.id == zoneId) it.copy(isActive = false) else it
        }
        logAudit("DANGER_ZONE_DEACTIVATED", actor, zoneId, "Deactivated danger zone")
    }

    // Safety Messages & Check-ins
    fun sendSafetyMessage(circleId: String, messageText: String, isQuickCheckIn: Boolean = false, locationTag: String? = null) {
        val user = _currentUser.value
        val msg = SafetyMessage(
            circleId = circleId,
            senderName = user.name,
            senderRole = user.role.displayName,
            message = messageText,
            isQuickCheckIn = isQuickCheckIn,
            locationTag = locationTag
        )
        _circleMessages.value = _circleMessages.value + msg
    }

    // Live Location Sharing
    fun startLiveShare(contactName: String, contactPhone: String, durationMinutes: Int, currentPoint: LocationPoint): LiveShareSession {
        val session = LiveShareSession(
            contactName = contactName,
            contactPhone = contactPhone,
            expiresAtEpoch = System.currentTimeMillis() + (durationMinutes * 60 * 1000L),
            isActive = true,
            currentPoint = currentPoint
        )
        _activeLiveShares.value = listOf(session) + _activeLiveShares.value
        logAudit("LIVE_SHARE_STARTED", _currentUser.value.name, session.id, "Shared live location with $contactName for $durationMinutes mins")
        return session
    }

    fun stopLiveShare(sessionId: String) {
        _activeLiveShares.value = _activeLiveShares.value.map {
            if (it.id == sessionId) it.copy(isActive = false) else it
        }
        logAudit("LIVE_SHARE_STOPPED", _currentUser.value.name, sessionId, "Revoked active live location session")
    }

    fun logAudit(action: String, actor: String, targetId: String, details: String) {
        val entry = AuditLogEntry(
            action = action,
            actor = actor,
            targetId = targetId,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        _auditLogs.value = listOf(entry) + _auditLogs.value
    }
}
