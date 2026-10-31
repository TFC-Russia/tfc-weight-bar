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

The required Java version differs per branch. `1.21.x` pins it in `.jabbarc`, so [jabba](https://github.com/shyiko/jabba) picks it up with no argument:

```
# on 1.21.x
jabba use
./gradlew build

# on 1.20.x
jabba use system@17
./gradlew build
```

Without jabba, point `JAVA_HOME` at a JDK of the version in the table above. The jar lands in `build/libs/`.

## Dependencies

TerraFirmaCraft and the Curios API are fetched by Gradle and need no manual download:

| Branch | Source | Versions |
| --- | --- | --- |
| `1.20.x` | CurseMaven, by CurseForge file id | TFC 3.2.7, Curios 5.10.0 |
| `1.21.x` | Modrinth maven, by version | TFC 4.2.12, Curios 9.5.1+1.21.1 |

[TFC Curios Weight](https://github.com/koenigstag/tfc-curios-weight) is the exception: it is read from `libs/` as a jar, because it is not published to any maven. The jar in the repository is built from that project, from the branch matching this one, and the file name is what `build.gradle` resolves:

| Branch | Jar in `libs/` |
| --- | --- |
| `1.20.x` | `tfc_curios_weight-1.0.0+1.20.1.jar` |
| `1.21.x` | `tfc_curios_weight-1.0.0+1.21.1.jar` |

To build against a changed TFC Curios Weight, build it there, drop the jar in `libs/` under the name above and bump `tfc_curios_weight_version` in `gradle.properties` if its version changed.

All other version numbers live in `gradle.properties` too. On `1.21.x` bumping a dependency means editing one line; on `1.20.x` it also means looking up the new CurseForge file id.

## Before opening a pull request

- `./gradlew build` passes on the branch you changed.
- `README.md` and `README.ru.md` still say the same thing.
- Code and comments are in English. Player facing text goes through the language files in `src/main/resources/assets/tfc_weight_bar/lang/`, not into the source.
