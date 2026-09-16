import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('checker', Path(__file__).parents[1] / 'scripts/check_commit.py')
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)


class CommitPolicyTest(unittest.TestCase):
    def test_normal_headers(self):
        for value in ['feat(pairing): add code entry', 'fix: restore saved scene', 'chore(repo): initialize projector player repository']:
            with self.subTest(value=value):
                self.assertTrue(checker.valid(value))

    def test_breaking_change_and_body(self):
        self.assertTrue(checker.valid('feat(protocol)!: change command format\n\nBREAKING CHANGE: protocol version 2'))
        self.assertTrue(checker.valid('fix!: drop unsafe fallback'))

    def test_rejects_unstructured_or_empty(self):
        for value in ['', 'update stuff', 'feat:', 'feat: ', 'feat(scope) description', 'feature: add thing', 'fix: trailing space ']:
            with self.subTest(value=value):
                self.assertFalse(checker.valid(value))


if __name__ == '__main__':
    unittest.main()
