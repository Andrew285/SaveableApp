package com.rainyday.saveableapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached title/thumbnail resolved for a URL, so a static link-preview card survives app restarts. */
@Entity(tableName = "link_previews")
data class LinkPreviewCacheEntity(
    @PrimaryKey val url: String,
    val title: String?,
    val imageUrl: String?,
    val fetchedAt: Long
)
