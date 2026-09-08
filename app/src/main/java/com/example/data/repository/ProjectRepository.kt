package com.example.data.repository

import com.example.data.db.ProjectDao
import com.example.data.db.ProjectEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val recentProjects: Flow<List<ProjectEntity>> = projectDao.getRecentProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun saveProject(project: ProjectEntity): Long = projectDao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun deleteProject(id: Long) = projectDao.deleteProjectById(id)

    suspend fun clearAll() = projectDao.deleteAllProjects()
}
