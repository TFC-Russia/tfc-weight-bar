#!/usr/bin/env bash
# Releases the mod from the release branch you are on: sets mod_version, commits
# it, pushes the branch and creates the GitHub release. The Publish workflow then
# builds the jar and uploads it to CurseForge. See CONTRIBUTING.md, "Releasing".
#
#   bash scripts/release.sh <version> [--prerelease] [--dry-run] [--yes]
#
# <version> is the mod's own version, such as 1.0.2. The script appends
# +<minecraft_version> from gradle.properties, as VERSIONING.md describes.

set -euo pipefail

usage() { sed -n '2,9p' "$0" | sed -E 's/^# ?//'; exit 2; }
die() { echo "release: $*" >&2; exit 1; }

version="" prerelease=false dry_run=false yes=false
for arg in "$@"; do
  case "$arg" in
    --prerelease) prerelease=true ;;
    --dry-run) dry_run=true ;;
    --yes) yes=true ;;
    -h|--help) usage ;;
    -*) die "unknown option $arg" ;;
    *) [ -z "$version" ] || die "expected one version, got $version and $arg"; version=$arg ;;
  esac
done
[ -n "$version" ] || usage
[[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "the version looks like 1.0.2, without the game version: $version"

cd "$(git rev-parse --show-toplevel)"

prop() { grep -E "^$1=" gradle.properties | cut -d= -f2- | tr -d '\r'; }
minecraft_version=$(prop minecraft_version)
old_version=$(prop mod_version)
new_version="$version+$minecraft_version"
tag="v$new_version"

# Only the release branches go out; any other branch stays on this machine
branch=$(git branch --show-current)
[[ $branch =~ ^1\.[0-9]+\.x$ ]] || die "not on a release branch such as 1.20.x or 1.21.x: ${branch:-detached HEAD}"
[ -z "$(git status --porcelain)" ] || die "the working tree has uncommitted changes"

git fetch --quiet origin "$branch"
behind=$(git rev-list --count "HEAD..origin/$branch")
[ "$behind" -eq 0 ] || die "$branch is $behind commits behind origin/$branch, pull first"
ahead=$(git rev-list --count "origin/$branch..HEAD")

old_base=${old_version%%+*}
newest=$(printf '%s\n' "$old_base" "$version" | sort -V | tail -n 1)
[ "$version" != "$old_base" ] && [ "$newest" = "$version" ] || die "$version is not newer than the current $old_version"
[ -z "$(git ls-remote --tags origin "refs/tags/$tag")" ] || die "the tag $tag already exists on origin"

# The changelog entry of this version becomes the release notes and the CurseForge changelog
changelog() {
  awk -v heading="## $version" '
    { sub(/\r$/, "") }
    $0 == heading { found = 1; next }
    found && /^#/ { exit }
    found && /^$/ { blank++; next }
    found { while (blank > 0) { print ""; blank-- } print }
  ' "$1"
}
notes=$(changelog CHANGELOG.md)
[ -n "$notes" ] || die "CHANGELOG.md has no '## $version' entry: add it, commit it and run again"
[ -n "$(changelog CHANGELOG.ru.md)" ] || die "CHANGELOG.ru.md has no '## $version' entry: add it, commit it and run again"

gh auth status >/dev/null 2>&1 || die "the GitHub CLI is not signed in, run gh auth login"

branch_line=$branch
[ "$ahead" -eq 0 ] || branch_line="$branch, with unpushed commits that go out too: $ahead"
release_line=$tag
release_flags=()
if $prerelease; then
  release_line="$tag (pre-release, a beta on CurseForge)"
  release_flags+=(--prerelease)
fi

echo "Branch:   $branch_line"
echo "Version:  $old_version -> $new_version"
echo "Release:  $release_line"
echo "Notes:"
sed 's/^/  /' <<<"$notes"

if $dry_run; then
  echo "Dry run, nothing changed."
  exit 0
fi
if ! $yes; then
  read -r -p "Commit the version, push $branch and publish $tag? [y/N] " answer
  [[ $answer =~ ^[Yy]$ ]] || die "stopped, nothing changed"
fi

sed -i -E "s/^mod_version=.*/mod_version=$new_version/" gradle.properties
[ "$(prop mod_version)" = "$new_version" ] || die "could not set mod_version in gradle.properties"
git commit --quiet -m "Bump the version to $version" -- gradle.properties
git push --quiet origin "$branch"

gh release create "$tag" --target "$branch" --title "$new_version" --notes "$notes" ${release_flags[@]+"${release_flags[@]}"} \
  || die "$branch is pushed, but creating the release failed. Retry with: gh release create $tag --target $branch --title $new_version"

echo "The Publish workflow now builds $tag and uploads it to CurseForge: gh run list --workflow publish.yml"
