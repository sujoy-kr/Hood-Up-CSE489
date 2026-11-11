# Unattended runner (current behavior)

Run `python3 run_prompts.py` from this directory. Existing `.prompt-runner/state.json`
and journals are reused; do not delete them to resume. Only successful prompts advance.
No terminal questions or alarms are used. Recoverable blockers are sent back to the
agent for autonomous diagnosis, execution of checks, and repair. Missing credentials,
console access, and device tests cannot be fabricated; an unresolved blocker may
keep retrying until interrupted or the CLI fails. Ctrl+C preserves progress.
Connection, usage, malformed output, and other CLI failures save progress and exit;
rerun the same command after resolving them. Inspect attempt logs for the cause.

The default sandbox is workspace-write with approvals disabled. To allow the local
runner to build using host caches and network without approval prompts, explicitly use:

```sh
python3 run_prompts.py --sandbox danger-full-access
```

This grants child Codex processes host access. Launch from a terminal where `codex`
is installed and authenticated. `--codex /absolute/path/to/codex` selects its executable.
The parent application's sandbox still limits its children; launch in your normal
host terminal if the current application restricts filesystem or network access.

Earlier documentation below describes the original interactive runner and is superseded.

Run from this project directory on Linux/macOS with Python 3.10+ and a logged-in Codex CLI:

```bash
codex login
python3 run_prompts.py
```

If `codex` is missing, install the official Codex CLI first (for example `npm install -g @openai/codex`). This script uses the CLI's existing authentication; it does not read your `.env` or need a separate API key.

The runner sends the 16 fenced prompts from `PROMPTS.md`, in order, through `codex exec --json`. It keeps the same Codex conversation using `exec resume`. The prompts target Codex: Prompt 0 creates `AGENTS.md` with the project rules, and later prompts follow it. No existing `AGENTS.md` is required to start. The other fenced troubleshooting text is excluded.

At first launch, finish the manual prerequisites at the top of `PROMPTS.md` and type `ready`. After that, it advances automatically only after a completed Codex turn reports structured success. Codex is instructed to run required checks and report blockers. Success is Codex's assessment, not an independent guarantee of app correctness; use the recommended phone/emulator tests too.

Questions, blocked tasks, network/usage failures, malformed results, and 15 minutes without output pause the sequence. A repeating audible tone continues until you submit a reply. Type your answer/instructions, `/retry` after resolving an error, or `/quit` to exit. Empty answers retry the pending prompt. The tone uses `paplay`, `aplay`, or `afplay`; without a player it uses the terminal bell, which some terminals mute. Keep system audio enabled. Install a supported player and test your volume before leaving the computer unattended. `--silent` disables sound.

Codex runs with the workspace-write sandbox and its existing approval configuration. Non-interactive execution cannot display every interactive approval dialog: denied actions should be reported as blockers, and a stalled process triggers the idle alert. This is not a monitor for a separate Codex GUI session.

After interruption, run exactly the same command:

```bash
python3 run_prompts.py
```

State, session ID, your last reply, and durable event logs live in `.prompt-runner/`. Do not delete that directory or the CLI's saved sessions. A completed result recovered from a log is checkpointed before moving on. Incomplete work resumes the current prompt and asks Codex to inspect existing files before continuing. This preserves the prompt position and conversation, not an exact instruction pointer inside a shell command. External actions already performed cannot be rolled back or guaranteed to run exactly once. Avoid deleting state or manually running another copy against the same project. A lock prevents simultaneous runners using the same state directory. On Linux, a saved child PID also prevents resuming while an orphaned Codex process is still present.

Changing prompt contents or the working directory fails closed to avoid applying old progress to new tasks. Restore the original input, or deliberately use a new `--state-dir` to start a separate sequence.

Other options:

```bash
python3 run_prompts.py --list
python3 run_prompts.py --idle-timeout 1800
python3 run_prompts.py --codex /path/to/codex --model YOUR_MODEL
python3 -m unittest discover -s tests -v
```

Keep `.prompt-runner/` out of source control and deliverable archives: transcripts can contain project information or secrets printed by commands. Runtime state uses atomic replacements and fsync, though disk loss/corruption still requires backups. Ctrl+C stops the active Codex process and preserves the pending prompt. The runner alerts and waits on failures rather than repeatedly consuming quota.

Official interface reference: [Codex non-interactive mode](https://learn.chatgpt.com/docs/non-interactive-mode).
