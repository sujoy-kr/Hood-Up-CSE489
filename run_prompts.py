#!/usr/bin/env python3
"""Sequential, resumable Codex CLI runner. Python 3.10+, no dependencies."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import re
import shutil
import signal
import subprocess
import sys
import threading
import time


def atomic_json(path, value):
    tmp = path.with_suffix('.tmp')
    with tmp.open('w', encoding='utf-8') as f:
        json.dump(value, f, indent=2, ensure_ascii=False)
        f.flush()
        os.fsync(f.fileno())
    os.replace(tmp, path)
    if os.name == 'posix':
        fd = os.open(path.parent, os.O_RDONLY)
        try:
            os.fsync(fd)
        finally:
            os.close(fd)


def load_prompts(path):
    text = path.read_text(encoding='utf-8')
    sections = re.split(r'(?m)^## Prompt (\d+)\s*[^\n]*\n', text)
    prompts = []
    for i in range(1, len(sections), 2):
        blocks = re.findall(r'(?ms)^```text\s*\n(.*?)^```\s*$', re.split(r'(?m)^## ', sections[i + 1])[0])
        if len(blocks) != 1:
            raise ValueError(f'Prompt {sections[i]} must contain exactly one ```text block')
        prompts.append({'number': int(sections[i]), 'text': blocks[0].strip()})
    if not prompts or [p['number'] for p in prompts] != list(range(len(prompts))):
        raise ValueError('Expected consecutive numbered prompts beginning with Prompt 0')
    return prompts


def journal_result(path):
    """Recover completed output even if killed before checkpointing the result."""
    session, final, completed, failed = None, None, False, False
    if path.exists():
        for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
            try:
                event = json.loads(line)
            except ValueError:
                continue
            kind = event.get('type')
            if kind == 'thread.started':
                session = event.get('thread_id')
            if kind == 'item.completed' and event.get('item', {}).get('type') == 'agent_message':
                final = event['item'].get('text', '')
            if kind == 'turn.completed':
                completed = True
            if kind in ('turn.failed', 'error'):
                failed = True
    result = None
    if completed and not failed and final:
        try:
            result = json.loads(final)
            if result.get('status') not in ('success', 'needs_input', 'blocked'):
                result = None
        except (ValueError, AttributeError):
            result = None
    return session, result


def process_identity(pid):
    # Linux start time distinguishes a live child from a later reuse of its PID.
    try:
        return Path(f'/proc/{pid}/stat').read_text().rsplit(')', 1)[1].split()[19]
    except (OSError, IndexError):
        return None


def stop_process(proc):
    if proc.poll() is not None:
        return
    if os.name == 'posix':
        os.killpg(proc.pid, signal.SIGTERM)
    else:
        proc.terminate()
    try:
        proc.wait(timeout=5)
    except subprocess.TimeoutExpired:
        if os.name == 'posix':
            os.killpg(proc.pid, signal.SIGKILL)
        else:
            proc.kill()
        proc.wait()


def run_attempt(args, state, save, directory, prompt):
    number = len(list(directory.glob('attempt-*.jsonl'))) + 1
    log = directory / f'attempt-{number:06d}.jsonl'
    cmd = [args.codex, 'exec', '--json', '-c', 'approval_policy="never"',
           '--sandbox', getattr(args, 'sandbox', 'workspace-write'),
           '--skip-git-repo-check']
    if args.model:
        cmd += ['--model', args.model]
    if state.get('session'):
        cmd += ['resume', state['session']]
    cmd += ['-']
    # Prepare durable intent before launching a process that can change project files.
    state.update(attempt=log.name, phase='running')
    save()
    print(f'\nRunning Prompt {state["index"]}; log: {log}', flush=True)
    proc = subprocess.Popen(cmd, cwd=args.workdir, stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, encoding='utf-8', errors='replace',
                            start_new_session=(os.name == 'posix'))
    state['child'] = {'pid': proc.pid, 'identity': process_identity(proc.pid)}
    save()
    lines = queue.Queue()

    def read_output():
        try:
            for line in proc.stdout:
                lines.put(line)
        finally:
            lines.put(None)

    threading.Thread(target=read_output, daemon=True).start()
    try:
        proc.stdin.write(prompt)
        proc.stdin.close()
        last = time.monotonic()
        with log.open('a', encoding='utf-8') as f:
            while True:
                try:
                    line = lines.get(timeout=1)
                except queue.Empty:
                    if time.monotonic() - last > args.idle_timeout:
                        print('No output before idle timeout; saving for resume.', flush=True)
                        stop_process(proc)
                        break
                    continue
                if line is None:
                    break
                last = time.monotonic()
                f.write(line)
                f.flush()
                os.fsync(f.fileno())
                print(line.rstrip(), flush=True)
                try:
                    event = json.loads(line)
                    if event.get('type') == 'thread.started':
                        state['session'] = event['thread_id']
                        save()
                except (ValueError, KeyError):
                    pass
        proc.wait()
    finally:
        stop_process(proc)
        proc.stdout.close()
    session, result = journal_result(log)
    if session:
        state['session'] = session
    state.update(result=result, phase='result', child=None, exit_code=proc.returncode)
    save()


CONTRACT = '''\n\nRunner instructions:
Execute only the current numbered prompt. Read AGENTS.md if it exists as project
instructions. Prompt 0 creates it; it need not exist before Prompt 0. Inspect existing work before making changes;
this may be a recovery of an interrupted attempt. Do not redo completed work blindly.
Run the prompt's checks and fix failures before claiming success. For Prompt 0 respect
its instruction to change only AGENTS.md. For later code changes run
./gradlew assembleDebug. If credentials, decisions, permissions, console actions,
device testing, or other human actions are unavailable, record the blocker accurately.
Never ask interactive questions. Execute all available scripts and checks yourself.
Fix build failures and inspect logs yourself rather than asking the user to run commands. Never claim a check passed without running it. Never advance to the next prompt.
Your final response MUST be a single JSON object (no markdown fences):
{"status":"success|needs_input|blocked", "summary":"what happened and checks run",
 "question":"specific input/action required, or empty string"}.
Use success only when this prompt is fully completed and its available automated
checks pass. Mention device tests still recommended in summary. Use blocked for unavailable external prerequisites; never invent credentials or test results.
For recoverable code/build issues keep working autonomously. The runner will retry this
prompt until success or a CLI/connection/usage failure. User authorizes sequential
execution of all numbered prompts, but each turn handles only its current prompt.
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--prompts', type=Path, default=Path('PROMPTS.md'))
    parser.add_argument('--workdir', type=Path, default=Path.cwd())
    parser.add_argument('--state-dir', type=Path, default=Path('.prompt-runner'))
    parser.add_argument('--codex', default='codex')
    parser.add_argument('--model')
    parser.add_argument('--idle-timeout', type=float, default=900,
                        help='Seconds without Codex output before stopping for human input')
    parser.add_argument('--silent', action='store_true', help=argparse.SUPPRESS)
    parser.add_argument('--sandbox', choices=('workspace-write', 'danger-full-access'),
                        default='workspace-write')
    parser.add_argument('--list', action='store_true', help='List prompts without running Codex')
    args = parser.parse_args()
    args.workdir = args.workdir.resolve()
    args.prompts = (args.workdir / args.prompts).resolve()
    directory = (args.workdir / args.state_dir).resolve()
    prompts = load_prompts(args.prompts)
    digest = hashlib.sha256(json.dumps(prompts, sort_keys=True).encode()).hexdigest()
    if args.list:
        for p in prompts:
            print(f'Prompt {p["number"]}: {p["text"].splitlines()[0]}')
        return
    if args.idle_timeout <= 0:
        parser.error('--idle-timeout must be positive')
    directory.mkdir(parents=True, exist_ok=True)
    # flock releases automatically on crashes, including SIGKILL.
    import fcntl
    with (directory / 'runner.lock').open('w') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            parser.error('Another runner is using this state directory')
        path = directory / 'state.json'
        state = json.loads(path.read_text()) if path.exists() else {
            'version': 1, 'digest': digest, 'workdir': str(args.workdir),
            'index': 0, 'session': None, 'phase': 'ready', 'reply': '',
            'prerequisites_confirmed': False}
        if state['digest'] != digest or state['workdir'] != str(args.workdir):
            parser.error('Prompts or workdir changed. Restore them or use a different --state-dir.')
        save = lambda: atomic_json(path, state)
        save()
        if not shutil.which(args.codex):
            parser.error('Codex CLI not found. Install it and run codex login first.')
        # Prerequisites are checked by the agent without interactive questions.
        while state['index'] < len(prompts):
            if state['phase'] == 'running':
                child = state.get('child')
                if child and child.get('identity') and process_identity(child['pid']) == child['identity']:
                    parser.error(f"Previous Codex process {child['pid']} is still present. "
                                 'Wait for it to finish or stop it before resuming.')
                session, result = journal_result(directory / state['attempt'])
                state.update(session=session or state.get('session'), result=result, phase='result')
                save()
            if state['phase'] == 'result':
                result = state.get('result')
                if result and result['status'] == 'success':
                    print(f'Prompt {state["index"]} completed: {result.get("summary", "")}', flush=True)
                    state['index'] += 1
                    state.update(phase='ready', reply='', result=None)
                    save()
                    continue
                if result is None:
                    state['stop_reason'] = 'Connection/usage/CLI failure or interrupted output; inspect attempt log.'
                    state.update(phase='ready', child=None)
                    save()
                    print(state['stop_reason'] + ' Progress saved; rerun to resume.', flush=True)
                    return 2
                # Continue the same prompt, giving the agent its previous blocker.
                state.update(reply=json.dumps(result), phase='ready')
                save()
            p = prompts[state['index']]
            text = f'Current task: Prompt {p["number"]}\n\n{p["text"]}' + CONTRACT
            if state.get('attempt'):
                text += '\nThis run follows earlier work. Inspect what is already complete first.\n'
            if state.get('reply'):
                text += '\nPrevious checkpoint/context (not a new human response):\n' + state['reply']
            run_attempt(args, state, save, directory, text)
        print('All prompts completed. Progress and logs are saved in ' + str(directory))
        return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print('\nStopped. Progress saved; run the same command to resume.', file=sys.stderr)
        sys.exit(130)
    except (OSError, ValueError) as exc:
        print(f'Runner stopped: {exc}. Existing progress is preserved.', file=sys.stderr)
        sys.exit(1)
