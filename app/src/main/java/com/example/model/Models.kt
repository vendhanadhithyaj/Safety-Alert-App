package com.example.model

import java.util.UUID

enum class UserRole(val displayName: String, val badge: String) {
    PUBLIC("People / Student / Citizen", "Civilian"),
    RESPONDER("Authorized Responder", "Police / Rescue"),
    ADMIN("Safety Administrator", "Admin")
}

data class UserProfile(
    val id: String = "user-default-1",
    val name: String = "Kavitha Ramanathan",
    val phone: String = "+91 98401 23456",
    val role: UserRole = UserRole.PUBLIC,
    val institution: String = "Kumbakonam Central Higher Secondary",
    val district: String = "Thanjavur (Kumbakonam)",
    val responderBadgeId: String? = null,
    val trustedContacts: List<String> = listOf("+91 94431 88990 (Parent)", "+91 98842 55667 (Guardian)")
)

enum class SosStatus(val label: String, val description: String) {
    CREATED("Created", "Incident created locally with timestamp"),
    SAVED_ON_DEVICE("Saved Locally", "Persisted in secure device storage"),
    SENT_TO_SERVER("Sent to Server", "Transmitted to SAFEGUARD network"),
    QUEUED("Notification Queued", "Dispatched to regional emergency queue"),
    ACKNOWLEDGED("Acknowledged", "Authorized responder assigned to incident"),
    RESOLVED("Resolved", "Incident verified safe and closed")
}

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val landmark: String,
    val district: String = "Thanjavur",
    val state: String = "Tamil Nadu"
) {
    val googleMapsUrl: String
        get() = "https://maps.google.com/?q=$latitude,$longitude"
}

data class SosIncident(
    val id: String = "SG-TN-${System.currentTimeMillis() % 100000}",
    val userId: String,
    val userName: String,
    val userPhone: String,
    val emergencyType: String,
    val location: LocationPoint,
    val timestamp: Long = System.currentTimeMillis(),
    val status: SosStatus = SosStatus.CREATED,
    val responderNotes: String? = null,
    val acknowledgedBy: String? = null,
    val resolvedAt: Long? = null
)

enum class ReportCategory(val displayName: String, val iconName: String) {
    HARASSMENT("Harassment / Suspicious Activity", "security"),
    UNLIT_ROAD("Unlit Road / Faulty Streetlight", "lightbulb"),
    TRAFFIC_HAZARD("Accident / Traffic Hazard", "traffic"),
    WATERLOGGING("Waterlogging / Flood Hazard", "water"),
    MEDICAL_HAZARD("Medical Emergency / Animal Threat", "medical_services"),
    MISSING_PERSON("Missing Child / Student Alert", "person_search")
}

enum class ReviewStatus(val displayName: String) {
    PENDING_REVIEW("Pending Review"),
    UNDER_REVIEW("Under Review"),
    VERIFIED("Verified & Active"),
    REJECTED("Rejected"),
    INFO_REQUESTED("Info Requested")
}

data class CommunityReport(
    val id: String = "REP-TN-${System.currentTimeMillis() % 100000}",
    val reporterId: String,
    val reporterName: String,
    val category: ReportCategory,
    val title: String,
    val description: String,
    val location: LocationPoint,
    val timestamp: Long = System.currentTimeMillis(),
    val photoUri: String? = null,
    val audioDurationSec: Int = 0,
    val status: ReviewStatus = ReviewStatus.PENDING_REVIEW,
    val rejectionReason: String? = null,
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val isDangerZoneCreated: Boolean = false
)

enum class ZoneSeverity { HIGH, MEDIUM, LOW }

data class DangerZone(
    val id: String = "DZ-${UUID.randomUUID().toString().take(6).uppercase()}",
    val title: String,
    val description: String,
    val category: ReportCategory,
    val location: LocationPoint,
    val radiusMeters: Int = 300,
    val severity: ZoneSeverity = ZoneSeverity.HIGH,
    val verifiedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L * 3, // 3 days default
    val isActive: Boolean = true,
    val approvedByAuthority: String = "Tamil Nadu Police / Kumbakonam Taluk"
)

data class SafetyMessage(
    val id: String = UUID.randomUUID().toString(),
    val circleId: String,
    val senderName: String,
    val senderRole: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isQuickCheckIn: Boolean = false,
    val locationTag: String? = null
)

data class EmergencyContact(
    val name: String,
    val number: String,
    val category: String, // "National", "Tamil Nadu Official", "Kumbakonam Local", "Personal"
    val description: String,
    val isVerified: Boolean = true,
    val verifiedDate: String = "Verified Oct 2026"
)

data class AuditLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val action: String,
    val actor: String,
    val targetId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveShareSession(
    val id: String = UUID.randomUUID().toString().take(8),
    val contactName: String,
    val contactPhone: String,
    val expiresAtEpoch: Long,
    val isActive: Boolean = true,
    val currentPoint: LocationPoint
)

enum class BuddyRelation(val label: String) {
    PARENT("Parent / Guardian"),
    SIBLING("Sibling / Family"),
    CLASSMATE("Classmate / Student"),
    TEACHER("Teacher / Faculty"),
    FRIEND("Trusted Friend"),
    COMMUNITY_WATCH("Community Volunteer")
}

enum class BuddyStatus(val label: String) {
    ACTIVE("Active Guardian"),
    PENDING_INVITE("Invite Sent"),
    INVITATION_RECEIVED("Incoming Request")
}

data class BuddyMember(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val phone: String,
    val relation: BuddyRelation,
    val status: BuddyStatus = BuddyStatus.ACTIVE,
    val isTrustedForSos: Boolean = true,
    val canViewLiveLocation: Boolean = true,
    val invitedAt: Long = System.currentTimeMillis() - 86400000L * 2,
    val acceptedAt: Long? = System.currentTimeMillis() - 86400000L,
    val lastCheckIn: String = "15m ago • Near Kumbakonam Railway Road",
    val inviteCode: String? = null
)

data class BuddySosAlert(
    val incidentId: String,
    val victimName: String,
    val victimPhone: String,
    val location: LocationPoint,
    val timestamp: Long = System.currentTimeMillis(),
    val emergencyType: String = "Physical Threat / Distress SOS",
    val batteryPercent: Int = 82,
    val isAcknowledged: Boolean = false,
    val acknowledgedByMe: Boolean = false,
    val responseType: String? = null, // "ON_MY_WAY", "CALLED_POLICE_112", "REACHED_VICTIM"
    val responderNotes: String? = null,
    val activeSirenAlert: Boolean = true
)

enum class SafeStationType(val label: String) {
    POLICE("Police Station / Outpost"),
    HOSPITAL("Hospital / Emergency Care"),
    WOMEN_HELP_DESK("All-Women Safety Desk"),
    SHELTER("Safe Haven / Shelter")
}

data class SafeStation(
    val id: String,
    val name: String,
    val type: SafeStationType,
    val location: LocationPoint,
    val phone: String,
    val is24Hours: Boolean = true,
    val jurisdiction: String = "Tamil Nadu Police / Health Dept"
)
