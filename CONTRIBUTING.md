# Contributing

Pull requests are welcome, and so are issues: a suggestion or a bug report is just as useful.

## Branches

The two game versions live side by side, each on its own branch. There is no `main`.

| Branch | Minecraft | Loader | Java |
| --- | --- | --- | --- |
| `1.20.x` | 1.20.1 | Forge 47.3.0 | 17 |
| `1.21.x` | 1.21.1 | NeoForge 21.1.265 | 21 |

Open a pull request against the branch your change belongs to. For a change that applies to both, one pull request per branch is easiest to review.

## Building

The required Java version differs per branch. Each branch pins it in `.jabbarc`, so [jabba](https://github.com/shyiko/jabba) picks it up with no argument:

```
jabba use
./gradlew build
```

Without jabba, point `JAVA_HOME` at a JDK of the version in the table above. The jar lands in `build/libs/`.

## Dependencies

TerraFirmaCraft and the Curios API are fetched by Gradle and need no manual download. Only TerraFirmaCraft is required to play; the Curios API and TFC Curios Weight are optional, and the code reaches them only through `CuriosCompat` and `CuriosWeightCompat`:

| Branch | Source | Versions |
| --- | --- | --- |
| `1.20.x` | CurseMaven, by CurseForge file id | TFC 3.2.7, Curios 5.10.0 |
| `1.21.x` | Modrinth maven, by version | TFC 4.2.12, Curios 9.5.1+1.21.1 |

Gradle also fetches Patchouli, which TFC requires, but only to run the game with `./gradlew runClient`; the mod itself does not depend on it.

[TFC Curios Weight](https://github.com/TFC-Russia/tfc-curios-weight) is the exception: it is read from `libs/` as a jar, because it is not published to any maven. The jar in the repository is built from that project, from the branch matching this one, and the file name is what `build.gradle` resolves:

| Branch | Jar in `libs/` |
| --- | --- |
| `1.20.x` | `tfc_curios_weight-1.0.0+1.20.1.jar` |
| `1.21.x` | `tfc_curios_weight-1.0.0+1.21.1.jar` |

To build against a changed TFC Curios Weight, build it there, drop the jar in `libs/` under the name above and bump `tfc_curios_weight_version` in `gradle.properties` if its version changed.

All other version numbers live in `gradle.properties` too. On `1.21.x` bumping a dependency means editing one line; on `1.20.x` it also means looking up the new CurseForge file id.

## Releasing

A GitHub release publishes the mod. `.github/workflows/publish.yml` builds the jar of the commit the release points at and uploads it to the CurseForge project (id 1776579) and to the release itself. `scripts/release.sh` sets the version and creates that release:

1. On the release branch, add a `## <version>` entry at the top of both `CHANGELOG.md` and `CHANGELOG.ru.md`, and commit it.
2. Run the script with the mod's own version, without the game version:

   ```
   bash scripts/release.sh 1.0.1
   ```

The script stops before changing anything when the branch is not a release branch, has uncommitted changes, is behind `origin`, when the version is not newer than `mod_version` or its tag already exists, or when either changelog lacks the entry. Otherwise it shows the plan and asks before it:

- sets `mod_version` to `1.0.1+<minecraft version>`, as [VERSIONING.md](VERSIONING.md) describes, and commits that;
- pushes the branch;
- creates the release `v1.0.1+<minecraft version>` with the `CHANGELOG.md` entry as its notes.

| Option | Effect |
| --- | --- |
| `--dry-run` | Run the checks and show the plan, change nothing |
| `--prerelease` | Publish a pre-release, which goes up to CurseForge as a beta |
| `--yes` | Skip the question |

The first release, 1.0.0, is the version already in `gradle.properties`, so the script, which only moves the version forward, does not make it: create it by hand with `gh release create v1.0.0+<minecraft version> --target <branch> --title 1.0.0+<minecraft version> --notes-file <the 1.0.0 entry>`.

The script needs git and the GitHub CLI (`gh`) signed in to an account that can push to the repository. On Windows, run it from Git Bash.

The release notes become the CurseForge changelog. TerraFirmaCraft is listed there as a required dependency, the Curios API and TFC Curios Weight as optional ones. When the tag and `mod_version` disagree, the workflow stops before uploading anything. The upload needs a CurseForge API token in the `CURSEFORGE_TOKEN` repository secret.

The description of the page is published by nothing at all: the CurseForge token cannot write it. It lives in [`mod-description.md`](mod-description.md), so that a change to it is reviewable, and it is pasted into the page by hand under Manage Project → Description. The same text is on both branches, because one page covers both game versions.

To only build the jar on CI, run the workflow by hand: Actions → Publish → Run workflow.

## Before opening a pull request

- `./gradlew build` passes on the branch you changed.
- `README.md` and `README.ru.md` still say the same thing, and so do `CHANGELOG.md` and `CHANGELOG.ru.md`.
- Code and comments are in English. Player facing text goes through the language files in `src/main/resources/assets/tfc_weight_bar/lang/`, not into the source.
