package com.snowball.data

import com.snowball.data.backup.BackupService
import com.snowball.data.repo.CategoryRepository
import com.snowball.data.repo.DebtRepository
import com.snowball.data.repo.PaymentRepository
import com.snowball.data.repo.SettingsRepository
import com.snowball.db.SnowballDb

class Repos(db: SnowballDb) {
    val categories: CategoryRepository = CategoryRepository(db)
    val debts: DebtRepository = DebtRepository(db)
    val payments: PaymentRepository = PaymentRepository(db)
    val settings: SettingsRepository = SettingsRepository(db)
    val backup: BackupService = BackupService(db)
}
