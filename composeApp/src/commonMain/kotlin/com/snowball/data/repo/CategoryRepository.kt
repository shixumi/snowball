package com.snowball.data.repo

import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior
import com.snowball.db.SnowballDb
import kotlinx.datetime.Clock

class CategoryRepository(private val db: SnowballDb) {

    fun all(): List<Category> =
        db.categoryQueries.selectAll().executeAsList().map { row ->
            Category(
                id = row.id,
                name = row.name,
                isSystem = row.isSystem == 1L,
                behavior = CategoryBehavior.valueOf(row.behavior),
                iconKey = row.iconKey,
            )
        }

    fun byId(id: Long): Category? =
        db.categoryQueries.selectById(id).executeAsOneOrNull()?.let { row ->
            Category(row.id, row.name, row.isSystem == 1L, CategoryBehavior.valueOf(row.behavior), row.iconKey)
        }

    fun add(name: String, behavior: CategoryBehavior, iconKey: String = "") {
        db.categoryQueries.insert(
            name = name,
            isSystem = 0,
            behavior = behavior.name,
            iconKey = iconKey,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    fun setIcon(id: Long, iconKey: String) {
        db.categoryQueries.setIconById(iconKey = iconKey, id = id)
    }

    fun rename(id: Long, newName: String) {
        db.categoryQueries.renameById(name = newName, id = id)
    }

    fun delete(id: Long) {
        db.categoryQueries.deleteById(id)  // SQL itself guards isSystem = 0
    }
}
