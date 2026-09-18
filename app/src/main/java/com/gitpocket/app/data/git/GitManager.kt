package com.gitpocket.app.data.git

import android.content.Context
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PullResult(
    val success: Boolean,
    val ahead: Int,
    val behind: Int,
    val error: String? = null,
)

class GitManager(
    private val context: Context,
    private val credentialStore: CredentialStore,
) {

    private fun reposDir(): File = File(context.filesDir, "repos")

    fun localDir(repoId: Long): File = File(reposDir(), repoId.toString())

    private fun open(repoId: Long): Git = Git.open(File(localDir(repoId), ".git"))

    private fun credentials(repoId: Long): UsernamePasswordCredentialsProvider? {
        val token = credentialStore.getToken(repoId) ?: return null
        return UsernamePasswordCredentialsProvider(token, "")
    }

    suspend fun clone(url: String, token: String?, repoId: Long) {
        withContext(Dispatchers.IO) {
            val dir = localDir(repoId).apply { mkdirs() }
            if (!token.isNullOrBlank()) credentialStore.saveToken(repoId, token)
            Git.cloneRepository()
                .setURI(url)
                .setDirectory(dir)
                .setCredentialsProvider(credentials(repoId))
                .call()
                .close()
        }
    }

    suspend fun currentBranch(repoId: Long): String = withContext(Dispatchers.IO) {
        open(repoId).use { it.repository.branch }
    }

    suspend fun pull(repoId: Long): PullResult = withContext(Dispatchers.IO) {
        try {
            open(repoId).use { git ->
                val branch = git.repository.branch
                git.fetch()
                    .setRemote("origin")
                    .setCredentialsProvider(credentials(repoId))
                    .call()

                var (ahead, behind) = counts(git, branch)
                if (behind == 0) {
                    return@use PullResult(true, ahead, behind)
                }

                val remoteRef = git.repository.resolve("refs/remotes/origin/$branch")
                    ?: return@use PullResult(true, ahead, behind)

                if (ahead == 0) {
                    pullFastForward(git, remoteRef, ahead, behind)
                } else {
                    pullRebase(git, branch, ahead, behind)
                }
            }
        } catch (e: Exception) {
            PullResult(false, 0, 0, e.message ?: "Pull failed")
        }
    }

    private fun pullFastForward(
        git: Git,
        remoteRef: org.eclipse.jgit.lib.ObjectId,
        ahead: Int,
        behind: Int,
    ): PullResult {
        return try {
            val merge = git.merge()
                .include(remoteRef)
                .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
                .call()
            when (merge.mergeStatus) {
                MergeResult.MergeStatus.FAST_FORWARD,
                MergeResult.MergeStatus.ALREADY_UP_TO_DATE,
                MergeResult.MergeStatus.MERGED -> PullResult(true, 0, 0)
                MergeResult.MergeStatus.CHECKOUT_CONFLICT -> PullResult(
                    false, ahead, behind,
                    "Your local changes conflict with the remote. Commit or discard them first."
                )
                else -> PullResult(false, ahead, behind, merge.mergeStatus.toString())
            }
        } catch (e: Exception) {
            PullResult(false, ahead, behind, e.message ?: "Pull failed")
        }
    }

    private fun pullRebase(git: Git, branch: String, ahead: Int, behind: Int): PullResult {
        return try {
            val rebase = git.rebase()
                .setUpstream("refs/remotes/origin/$branch")
                .call()
            if (rebase.status == RebaseResult.Status.OK ||
                rebase.status == RebaseResult.Status.FAST_FORWARD
            ) {
                val (a, b) = counts(git, branch)
                PullResult(true, a, b)
            } else {
                runCatching {
                    git.rebase().setOperation(RebaseCommand.Operation.ABORT).call()
                }
                PullResult(false, ahead, behind, rebaseError(rebase.status))
            }
        } catch (e: Exception) {
            PullResult(false, ahead, behind, e.message ?: "Pull failed")
        }
    }

    suspend fun commitAndPush(
        repoId: Long,
        message: String,
        name: String,
        email: String,
    ): String? = withContext(Dispatchers.IO) {
        try {
            open(repoId).use { git ->
                val branch = git.repository.branch

                val config = git.repository.config
                config.setString("user", null, "name", name)
                config.setString("user", null, "email", email)
                config.save()

                git.add().addFilepattern(".").call()
                val status = git.status().call()
                val hasChanges = status.hasUncommittedChanges() || status.untracked.isNotEmpty()
                if (!hasChanges) return@use "No changes to commit"

                git.commit()
                    .setMessage(message)
                    .setAuthor(name, email)
                    .setCommitter(name, email)
                    .call()

                git.fetch()
                    .setRemote("origin")
                    .setCredentialsProvider(credentials(repoId))
                    .call()

                val remoteRef = git.repository.resolve("refs/remotes/origin/$branch")
                if (remoteRef != null) {
                    val (_, behind) = counts(git, branch)
                    if (behind > 0) {
                        val rebase = git.rebase()
                            .setUpstream("refs/remotes/origin/$branch")
                            .call()
                        if (rebase.status != RebaseResult.Status.OK &&
                            rebase.status != RebaseResult.Status.FAST_FORWARD
                        ) {
                            runCatching {
                                git.rebase().setOperation(RebaseCommand.Operation.ABORT).call()
                            }
                            return@use "Remote has conflicting changes. Pull and resolve manually."
                        }
                    }
                }

                git.push()
                    .setRemote("origin")
                    .setRefSpecs(RefSpec("HEAD:refs/heads/$branch"))
                    .setCredentialsProvider(credentials(repoId))
                    .call()
            }
            null
        } catch (e: Exception) {
            e.message ?: "Push failed"
        }
    }

    private fun counts(git: Git, branch: String): Pair<Int, Int> {
        val repo = git.repository
        val localRef = repo.exactRef("refs/heads/$branch") ?: return 0 to 0
        val remoteRef = repo.exactRef("refs/remotes/origin/$branch")
        val walk = RevWalk(repo)
        val localTip = walk.parseCommit(localRef.objectId)
        if (remoteRef == null) {
            walk.close()
            return 1 to 0
        }
        val remoteTip = walk.parseCommit(remoteRef.objectId)

        var ahead = 0
        walk.reset()
        walk.markStart(localTip)
        walk.markUninteresting(remoteTip)
        for (c in walk) ahead++

        var behind = 0
        walk.reset()
        walk.markStart(remoteTip)
        walk.markUninteresting(localTip)
        for (c in walk) behind++

        walk.close()
        return ahead to behind
    }

    private fun rebaseError(status: RebaseResult.Status): String = when (status) {
        RebaseResult.Status.UNCOMMITTED_CHANGES -> "You have uncommitted changes. Commit them before pulling."
        RebaseResult.Status.STOPPED,
        RebaseResult.Status.CONFLICTS,
        RebaseResult.Status.STASH_APPLY_CONFLICTS -> "Merge conflict while pulling. Reverted; resolve changes manually."
        else -> "Pull failed: $status"
    }
}