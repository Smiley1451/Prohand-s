package com.anand.prohands.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local storage for extra profile fields needed for AI wage prediction.
 * These fields are not stored in the backend but are required for the wage prediction API.
 */
@Entity(tableName = "user_wage_profile")
data class UserWageProfile(
    @PrimaryKey
    val userId: String,

    // Personal info
    val age: Int = 30,

    // Work experience
    val experienceYears: Int = 2,
    val workingHours: Int = 8,

    // Education
    val educationLevel: String = "secondary", // none, primary, secondary, higher secondary, diploma

    // Employment details
    val employmentType: String = "casual", // casual, contract, permanent, seasonal
    val sector: String = "construction", // construction, agriculture

    // Construction specific
    val jobRole: String = "helper", // helper, laborer, mason, electrician, plumber, carpenter, welder, painter, steel fixer
    val cityTier: String = "Tier-2", // Metro, Tier-1, Tier-2, Tier-3
    val projectType: String = "residential", // residential, commercial, infrastructure

    // Agriculture specific
    val occupation: String = "farm labourer", // farm labourer, harvester, ploughman, irrigation assistant, pesticide sprayer, tractor operator
    val state: String = "MH", // UP, MH, TN

    // Skill level (1-5)
    val skillLevel: Int = 3,

    // Timestamp
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Valid values for dropdown fields
 */
object WageProfileOptions {
    val educationLevels = listOf("none", "primary", "secondary", "higher secondary", "diploma")
    val employmentTypes = listOf("casual", "contract", "permanent", "seasonal")
    val sectors = listOf("construction", "agriculture")

    // Construction
    val jobRoles = listOf("helper", "laborer", "mason", "steel fixer", "painter", "plumber", "carpenter", "electrician", "welder")
    val cityTiers = listOf("Metro", "Tier-1", "Tier-2", "Tier-3")
    val projectTypes = listOf("residential", "commercial", "infrastructure")

    // Agriculture
    val occupations = listOf("farm labourer", "harvester", "ploughman", "irrigation assistant", "pesticide sprayer", "tractor operator")
    val states = listOf("UP", "MH", "TN")

    val skillLevels = listOf(1, 2, 3, 4, 5)
    val ageRange = (18..70).toList()
    val experienceRange = (0..50).toList()
    val workingHoursRange = (1..16).toList()
}

