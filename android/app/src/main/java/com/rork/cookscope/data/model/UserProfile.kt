package com.rork.cookscope.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole(val label: String) {
    CHEF_TEACHER("Chef / Teacher"),
    LEARNER("Learner"),
}

@Serializable
data class UserProfile(
    val id: String = "local_user",
    val username: String = "homechef",
    val displayName: String = "Home Chef",
    val bio: String = "Cooking with passion, one dish at a time",
    val profilePhotoUri: String? = null,
    val role: UserRole = UserRole.LEARNER,
    val starsReceived: Int = 0,
    val starsGiven: Int = 0,
    val posts: List<ChefPost> = emptyList(),
)

@Serializable
data class ChefPost(
    val id: String,
    val title: String,
    val caption: String,
    val imageUrl: String,
    val likes: Int = 0,
    val stars: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
)
