package com.example.android.codelabs.paging.di

import android.content.Context
import androidx.room.Room
import com.example.android.codelabs.paging.data.network.GithubService
import com.example.android.codelabs.paging.data.db.remotekeys.RemoteKeysDao
import com.example.android.codelabs.paging.data.db.repo.RepoDao
import com.example.android.codelabs.paging.data.db.RepoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logger).build()
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGithubService(retrofit: Retrofit): GithubService {
        return retrofit.create(GithubService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): RepoDatabase {
        return Room.databaseBuilder(appContext, RepoDatabase::class.java, "Github.db").build()
    }

    @Provides
    fun provideRepoDao(database: RepoDatabase): RepoDao {
        return database.reposDao()
    }

    @Provides
    fun provideRemoteKeysDao(database: RepoDatabase): RemoteKeysDao {
        return database.remoteKeysDao()
    }
}

