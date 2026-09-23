package id.my.faruq.coffegrader.data.repository

import id.my.faruq.coffegrader.data.local.dao.SampleInfoDao
import id.my.faruq.coffegrader.data.local.entity.SampleInfoEntity

class SampleRepository(
    private val dao: SampleInfoDao
) {
    suspend fun saveSample(sample: SampleInfoEntity): Long {
        return dao.insert(sample)
    }

    fun observeSamples() = dao.observeAllSamples()

    suspend fun getById(id: Long): SampleInfoEntity? = dao.getById(id)
}
