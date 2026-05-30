package com.example.data

import kotlinx.coroutines.flow.Flow

class QinglongConfigRepository(private val qinglongConfigDao: QinglongConfigDao) {
    val allConfigs: Flow<List<QinglongConfig>> = qinglongConfigDao.getAllConfigs()

    suspend fun getAllConfigsList(): List<QinglongConfig> {
        return qinglongConfigDao.getAllConfigsList()
    }

    suspend fun insert(config: QinglongConfig): Long {
        return qinglongConfigDao.insertConfig(config)
    }

    suspend fun update(config: QinglongConfig) {
        qinglongConfigDao.updateConfig(config)
    }

    suspend fun delete(config: QinglongConfig) {
        qinglongConfigDao.deleteConfig(config)
    }
}
