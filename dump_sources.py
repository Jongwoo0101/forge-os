from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent

SOURCE_ROOT = PROJECT_ROOT / "src" / "main"

OUTPUT = PROJECT_ROOT / "forgeOS_source_dump.md"


# 수집할 파일 확장자와 Markdown 코드블록 언어
SUPPORTED_EXTENSIONS = {
    ".java": "java",
    ".css": "css",
}


def collect():
    """저장소 기준 상대경로와 파일 경로 목록을 모은다."""
    if not SOURCE_ROOT.exists():
        return []

    files = []

    for path in SOURCE_ROOT.rglob("*"):
        if path.is_file() and path.suffix.lower() in SUPPORTED_EXTENSIONS:
            files.append(
                (path.relative_to(PROJECT_ROOT), path)
            )

    return sorted(files)


def main():
    files = collect()

    with OUTPUT.open("w", encoding="utf-8") as out:
        out.write("# ForgeOS Source Dump\n\n")
        out.write(f"총 소스 파일 수 : **{len(files)}개**\n\n")
        out.write("- 모듈 : `forgeOS` (os)\n")
        out.write("- 포함 확장자 : `.java`, `.css`\n")

        out.write("\n---\n\n## Files\n\n")

        for rel, _ in files:
            out.write(f"- `{rel}`\n")

        out.write("\n---\n\n")

        for index, (rel, path) in enumerate(files, start=1):
            language = SUPPORTED_EXTENSIONS[path.suffix.lower()]

            out.write(f"# {index}. {path.name}\n\n")
            out.write("**Path**\n")
            out.write(f"`{rel}`\n\n")
            out.write(f"```{language}\n")

            try:
                source = path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                source = path.read_text(
                    encoding="utf-8",
                    errors="replace"
                )

            out.write(source)

            if not source.endswith("\n"):
                out.write("\n")

            out.write("```\n\n---\n\n")

    print()
    print("완료!")
    print(f"총 {len(files)}개의 소스 파일을 저장했습니다.")
    print(f"출력 파일 : {OUTPUT}")


if __name__ == "__main__":
    main()