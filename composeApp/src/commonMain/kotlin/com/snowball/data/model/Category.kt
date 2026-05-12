package com.snowball.data.model

enum class CategoryBehavior { SCHEDULED, LEDGER }

data class Category(
    val id: Long,
    val name: String,
    val isSystem: Boolean,
    val behavior: CategoryBehavior,
)
