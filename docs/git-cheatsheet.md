# Git Cheat Sheet

## Setup
| Command | What it does |
|---|---|
| `git config --global user.name "Name"` | Set your name |
| `git config --global user.email "you@mail.com"` | Set your email |
| `git init` | Make current folder a repo |
| `git clone <url>` | Copy a remote repo |

## Status and history
| Command | What it does |
|---|---|
| `git status` | What changed, what is staged |
| `git log --oneline --graph --all` | Short history with branches |
| `git diff` | Unstaged changes |
| `git diff --staged` | Staged changes |
| `git show <commit>` | One commit in detail |

## Add and commit
| Command | What it does |
|---|---|
| `git add <file>` | Stage one file |
| `git add .` | Stage everything in this folder |
| `git add -p` | Stage piece by piece |
| `git restore --staged <file>` | Unstage a file |
| `git restore <file>` | Throw away local changes in a file |
| `git commit -m "feat(scope): message"` | Commit staged changes |
| `git commit --amend` | Fix last commit (only if not pushed) |

## Branches: create and switch
| Command | What it does |
|---|---|
| `git branch` | List local branches |
| `git branch -a` | List local + remote branches |
| `git branch <name>` | Create branch (stay where you are) |
| `git switch <name>` | Switch to branch |
| `git switch -c <name>` | Create and switch |
| `git branch -d <name>` | Delete merged branch |
| `git branch -D <name>` | Force delete branch |

## Remote: push and pull
| Command | What it does |
|---|---|
| `git remote -v` | Show remotes |
| `git fetch` | Download remote changes, don't touch your files |
| `git pull` | Fetch + merge into current branch |
| `git pull --rebase` | Fetch + rebase (cleaner history) |
| `git push` | Upload commits |
| `git push -u origin <branch>` | First push of a new branch |
| `git push origin --delete <branch>` | Delete remote branch |

## Merge
| Command | What it does |
|---|---|
| `git switch main` then `git merge <branch>` | Bring branch into main |
| `git merge --no-ff <branch>` | Always create a merge commit |
| `git merge --abort` | Cancel a merge with conflicts |

## Rebase
| Command | What it does |
|---|---|
| `git switch <branch>` then `git rebase main` | Replay your commits on top of main |
| `git rebase -i HEAD~3` | Edit / squash last 3 commits |
| `git rebase --continue` | After fixing a conflict |
| `git rebase --abort` | Cancel the rebase |

Rule: never rebase commits already pushed to a shared branch.

## Conflicts
1. `git status` shows conflicted files.
2. Open file, fix the `<<<<<<<` / `=======` / `>>>>>>>` parts.
3. `git add <file>`
4. `git commit` (merge) or `git rebase --continue` (rebase).

## Stash
| Command | What it does |
|---|---|
| `git stash` | Park changes |
| `git stash list` | Show parked changes |
| `git stash pop` | Bring back last stash |

## Undo
| Command | What it does |
|---|---|
| `git revert <commit>` | New commit that undoes one (safe after push) |
| `git reset --soft HEAD~1` | Undo last commit, keep changes staged |
| `git reset --hard HEAD~1` | Undo last commit and delete changes (dangerous) |
| `git reflog` | Find "lost" commits |

## Typical feature flow
```bash
git switch main
git pull
git switch -c feat/my-feature
git add .
git commit -m "feat(api): add endpoint"
git push -u origin feat/my-feature
```
