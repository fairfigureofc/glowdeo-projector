# Contributing

Use small feature branches and reviewable changes. All commits use Conventional Commits 1.0.0. Allowed types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert. Scopes are optional. The hook permits breaking-change `!` markers; include a descriptive BREAKING CHANGE footer when applicable.

Examples:
- feat(pairing): add projector pairing screen
- fix(player): recover after Wi-Fi loss
- test(mapping): cover nonrectangular TV masks
- chore(release): prepare 0.1.0-alpha.1

No app credentials, signing files, personal captures or media dumps in Git. Enable `.githooks` after cloning. CI also validates commit headers so a missing local hook does not bypass the convention. New release candidates require the documented Android and physical-device checks, not merely the repository tooling workflow.
