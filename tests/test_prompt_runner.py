import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import argparse
import run_prompts as runner


class RunnerTests(unittest.TestCase):
    def test_real_prompt_boundaries(self):
        prompts = runner.load_prompts(Path('PROMPTS.md'))
        self.assertEqual(len(prompts), 16)
        self.assertNotIn('If something breaks', prompts[-1]['text'])

    def test_durable_recovery_requires_completed_success(self):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / 'events.jsonl'
            events = [
                {'type': 'thread.started', 'thread_id': 'saved-session'},
                {'type': 'item.completed', 'item': {'type': 'agent_message',
                 'text': json.dumps({'status': 'success', 'summary': 'checked'})}}]
            log.write_text('\n'.join(map(json.dumps, events)) + '\n')
            self.assertEqual(runner.journal_result(log), ('saved-session', None))
            with log.open('a') as f:
                f.write(json.dumps({'type': 'turn.completed'}) + '\n')
                f.write('{truncated')
            self.assertEqual(runner.journal_result(log)[1]['status'], 'success')

    def test_unattended_retry_and_completion(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'PROMPTS.md').write_text('## Prompt 0 — Test\n```text\nDo work\n```\n')
            calls = []

            def attempt(args, state, save, directory, prompt):
                calls.append(prompt)
                status = 'needs_input' if len(calls) == 1 else 'success'
                state.update(phase='result', result={'status': status, 'summary': 'test'})
                save()

            with patch('sys.argv', ['runner', '--workdir', str(root)]), \
                    patch.object(runner.shutil, 'which', return_value='/fake/codex'), \
                    patch.object(runner, 'run_attempt', side_effect=attempt), \
                    patch('builtins.input', side_effect=AssertionError('interactive input')):
                self.assertEqual(runner.main(), 0)
            state = json.loads((root / '.prompt-runner/state.json').read_text())
            self.assertEqual(state['index'], 1)
            self.assertEqual(len(calls), 2)

    def test_fake_codex_failure_and_resume(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            fake = root / 'codex'
            fake.write_text('''#!/usr/bin/env python3
import json, sys
from pathlib import Path
prompt = sys.stdin.read()
assert 'current task' in prompt
if 'resume' not in sys.argv:
 print(json.dumps({'type':'thread.started','thread_id':'test-id'}), flush=True)
 print(json.dumps({'type':'turn.failed','error':{'message':'network lost'}}), flush=True)
 sys.exit(1)
assert sys.argv[sys.argv.index('resume')+1] == 'test-id'
print(json.dumps({'type':'item.completed','item':{'type':'agent_message','text':json.dumps({'status':'success','summary':'checks passed'})}}))
print(json.dumps({'type':'turn.completed'}))
''')
            fake.chmod(0o755)
            args = argparse.Namespace(codex=str(fake), model=None, workdir=root, idle_timeout=5)
            state = {'index': 0, 'session': None}
            save = lambda: runner.atomic_json(root / 'state.json', state)
            runner.run_attempt(args, state, save, root, 'current task')
            self.assertIsNone(state['result'])
            self.assertEqual(state['session'], 'test-id')
            runner.run_attempt(args, state, save, root, 'current task')
            self.assertEqual(state['result']['status'], 'success')
            self.assertEqual(len(list(root.glob('attempt-*.jsonl'))), 2)


if __name__ == '__main__':
    unittest.main()
