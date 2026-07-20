package cookscopeai.com.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String = "local_user",
    val username: String = "homechef",
    val displayName: String = "Home Chef",
    val bio: String = "Cooking with passion, one dish at a time",
    val profilePhotoUri: String? = null,
    val avatarUrl: String? = null,
)
