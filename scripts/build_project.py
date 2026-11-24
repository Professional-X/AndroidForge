#!/usr/bin/env python3
"""AndroidForge — Build Project.

Reads detection + setup JSON and runs the appropriate build command(s).

Strategy:
  1. If Flutter -> flutter pub get then flutter build apk.
  2. Otherwise -> run Gradle build tasks.
  3. Regenerate wrapper if missing.
  4. Write build result to GITHUB_OUTPUT.
  5. Exit non-zero on failure.
"""

from __future__ import annotations

import argparse
import json
import os
import shlex
import subprocess
import sys
import time
from pathlib import Path
from typing import Any


def clean_argument(value: Any) -> str:
    """Remove accidental surrounding single/double quotes from an argument."""

    value = str(value).strip()

    # Remove repeatedly nested matching quotes.
    while len(value) >= 2:
        if value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1].strip()
        else:
            break

    return value


def normalize_command(command: Any) -> list[str]:
    """Convert a command into clean subprocess arguments."""

    if isinstance(command, str):
        parts = shlex.split(command, posix=True)
    elif isinstance(command, (list, tuple)):
        parts = []

        for item in command:
            item = str(item).strip()

            # Handle arguments that contain nested quote wrappers.
            cleaned = clean_argument(item)

            # If an argument is still a quoted command fragment,
            # parse it only when it clearly contains multiple tokens.
            if (
                len(cleaned) >= 2
                and cleaned[0] in ("'", '"')
                and cleaned[-1] == cleaned[0]
            ):
                parts.extend(shlex.split(cleaned, posix=True))
            else:
                parts.append(cleaned)
    else:
        raise TypeError(
            f"Unsupported command type: {type(command).__name__}"
        )

    return [clean_argument(part) for part in parts if part.strip()]


def run(
    cmd: list[str],
    cwd: Path,
    log_file: Path,
    env: dict[str, str] | None = None,
) -> tuple[int, str]:
    """Run a command and stream output to console and log file."""

    cmd = normalize_command(cmd)

    if not cmd:
        print("ERROR: Empty command received.", flush=True)
        return 1, str(log_file)

    log_file.parent.mkdir(parents=True, exist_ok=True)

    print(f"\n=== Running: {shlex.join(cmd)}", flush=True)
    print(f"    cwd: {cwd}", flush=True)
    print(f"    log: {log_file}", flush=True)

    start = time.time()

    full_env = os.environ.copy()
    if env:
        full_env.update(env)

    with log_file.open(
        "w",
        encoding="utf-8",
        errors="replace",
    ) as f:

        f.write(f"$ {shlex.join(cmd)}\n")
        f.write(f"# cwd: {cwd}\n\n")
        f.flush()

        try:
            proc = subprocess.Popen(
                cmd,
                cwd=str(cwd),
                env=full_env,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1,
            )

            assert proc.stdout is not None

            for line in proc.stdout:
                sys.stdout.write(line)
                sys.stdout.flush()
                f.write(line)

            rc = proc.wait()

        except FileNotFoundError as exc:
            message = f"Command not found: {exc}\n"
            print(message, flush=True)
            f.write(message)
            rc = 127

        except Exception as exc:
            message = f"Command execution error: {exc}\n"
            print(message, flush=True)
            f.write(message)
            rc = 1

    elapsed = time.time() - start

    print(
        f"=== Exit {rc} after {elapsed:.1f}s",
        flush=True,
    )

    return rc, str(log_file)


def maybe_regenerate_wrapper(
    project_root: Path,
    gradle_version: str | None,
    log_dir: Path,
) -> bool:
    """Regenerate Gradle wrapper if its JAR is missing."""

    jar = (
        project_root
        / "gradle"
        / "wrapper"
        / "gradle-wrapper.jar"
    )

    if jar.exists():
        return True

    if not gradle_version:
        print(
            "WARNING: Cannot regenerate wrapper — "
            "no Gradle version specified",
            flush=True,
        )
        return False

    cmd = [
        "gradle",
        "wrapper",
        "--gradle-version",
        str(gradle_version),
        "--distribution-type",
        "bin",
    ]

    rc, _ = run(
        cmd,
        project_root,
        log_dir / "regenerate-wrapper.log",
    )

    return rc == 0 and jar.exists()


def main() -> int:

    parser = argparse.ArgumentParser(
        description="AndroidForge build project"
    )

    parser.add_argument(
        "--root",
        required=True,
        help="Project root path",
    )

    parser.add_argument(
        "--detect",
        required=True,
        help="Detection JSON (string or path)",
    )

    parser.add_argument(
        "--toolchain",
        required=True,
        help="Toolchain JSON (string or path)",
    )

    parser.add_argument(
        "--variant",
        default="auto",
        choices=["auto", "debug", "release", "bundle"],
    )

    parser.add_argument(
        "--log-dir",
        default=None,
        help="Directory to write logs",
    )

    parser.add_argument(
        "--output",
        default=None,
        help="Write JSON summary to this file",
    )

    args = parser.parse_args()

    def load(value: str) -> Any:
        path = Path(value)

        if path.exists():
            return json.loads(path.read_text(encoding="utf-8"))

        return json.loads(value)

    detect = load(args.detect)
    toolchain = load(args.toolchain)

    root = Path(args.root).resolve()

    if not root.exists() or not root.is_dir():
        print(
            f"ERROR: Project root does not exist: {root}",
            flush=True,
        )
        return 1

    log_dir = (
        Path(args.log_dir).resolve()
        if args.log_dir
        else root.parent / "androidforge-logs"
    )

    log_dir.mkdir(parents=True, exist_ok=True)

    project_type = detect.get("project_type", "unknown")

    needs_gradle = toolchain.get("needs_gradle", False)
    needs_flutter = toolchain.get("needs_flutter", False)
    use_wrapper = toolchain.get("use_wrapper", False)

    # Normalize commands before filtering or execution.
    raw_commands = toolchain.get("build_commands", [])
    raw_prep_commands = toolchain.get("prep_commands", []) or []

    all_commands = [
        normalize_command(command)
        for command in raw_commands
    ]

    prep_commands = [
        normalize_command(command)
        for command in raw_prep_commands
    ]

    all_commands = [
        command for command in all_commands if command
    ]

    prep_commands = [
        command for command in prep_commands if command
    ]

    # Filter commands according to requested variant.
    if args.variant != "auto":

        filtered = []

        for command in all_commands:

            cmd_str = " ".join(command).lower()

            if args.variant == "debug" and "debug" in cmd_str:
                filtered.append(command)

            elif args.variant == "release" and "release" in cmd_str:
                filtered.append(command)

            elif args.variant == "bundle" and "bundle" in cmd_str:
                filtered.append(command)

        if filtered:
            all_commands = filtered

    # Check or regenerate Gradle wrapper.
    if use_wrapper and project_type != "flutter":

        gradlew = root / "gradlew"

        if not gradlew.exists():

            print(
                "WARNING: gradlew missing despite detection — "
                "regenerating via system gradle",
                flush=True,
            )

            maybe_regenerate_wrapper(
                root,
                toolchain.get("gradle_version"),
                log_dir,
            )

    # Build environment.
    build_env: dict[str, str] = {}

    if needs_flutter:
        pass

    # Prepare commands.
    if prep_commands:

        print(
            f"\n=== Running {len(prep_commands)} prep command(s) "
            "— these do not count as build success ===",
            flush=True,
        )

    for original_command in prep_commands:

        cmd = list(original_command)

        if (
            use_wrapper
            and cmd
            and cmd[0].endswith("gradlew")
        ):

            gradlew_path = Path(cmd[0])

            if not gradlew_path.is_absolute():
                gradlew_path = root / gradlew_path

            if gradlew_path.exists():

                try:
                    gradlew_path.chmod(
                        gradlew_path.stat().st_mode | 0o111
                    )
                except Exception:
                    pass

                cmd = [
                    "/bin/sh",
                    str(gradlew_path),
                ] + cmd[1:]

        rc, log = run(
            cmd,
            root,
            log_dir / f"prep-{int(time.time())}-{len(cmd)}.log",
            build_env,
        )

        if rc != 0:

            print(
                f"WARNING: Prep command failed "
                f"(rc={rc}): {shlex.join(cmd)}",
                flush=True,
            )

        else:

            print(
                f"Prep command OK: {shlex.join(cmd)}",
                flush=True,
            )

    # Run build commands.
    success = False
    last_rc = 1
    last_log = ""

    successful_command: list[str] | None = None

    for original_command in all_commands:

        cmd = list(original_command)

        if (
            use_wrapper
            and cmd
            and cmd[0].endswith("gradlew")
        ):

            gradlew_path = Path(cmd[0])

            if not gradlew_path.is_absolute():
                gradlew_path = root / gradlew_path

            if gradlew_path.exists():

                try:
                    gradlew_path.chmod(
                        gradlew_path.stat().st_mode | 0o111
                    )
                except Exception:
                    pass

                cmd = [
                    "/bin/sh",
                    str(gradlew_path),
                ] + cmd[1:]

        # Final quote cleanup immediately before execution.
        cmd = normalize_command(cmd)

        print(
            f"\nBUILD TASK ARGUMENTS: {cmd}",
            flush=True,
        )

        rc, log = run(
            cmd,
            root,
            log_dir / f"build-{int(time.time())}-{len(cmd)}.log",
            build_env,
        )

        last_rc = rc
        last_log = log

        if rc == 0:

            success = True
            successful_command = cmd

            print(
                "BUILD SUCCEEDED.",
                flush=True,
            )

            break

    # Output JSON summary.
    result = {
        "project_root": str(root),
        "project_type": project_type,
        "needs_gradle": needs_gradle,
        "needs_flutter": needs_flutter,
        "build_succeeded": success,
        "successful_command": successful_command,
        "exit_code": last_rc,
        "log_file": last_log,
        "prep_commands_run": prep_commands,
        "build_commands_attempted": all_commands,
    }

    output = json.dumps(result, indent=2)

    if args.output:

        Path(args.output).write_text(
            output,
            encoding="utf-8",
        )

    else:

        print(output)

    # GitHub Actions output.
    gh_output = os.environ.get("GITHUB_OUTPUT")

    if gh_output:

        with open(
            gh_output,
            "a",
            encoding="utf-8",
        ) as f:

            f.write(
                f"build_succeeded="
                f"{'true' if success else 'false'}\n"
            )

            f.write(f"exit_code={last_rc}\n")
            f.write(f"log_file={last_log}\n")

            if successful_command:

                f.write(
                    "successful_command="
                    f"{shlex.join(successful_command)}\n"
                )

            f.write("json<<EOF\n")
            f.write(json.dumps(result))
            f.write("\nEOF\n")

    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
