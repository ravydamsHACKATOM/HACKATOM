/**
 * Commit message rules, enforced on every pull request by the CI.
 * Format: <type>(<scope>): <summary>, in English, imperative mood.
 */
export default {
  extends: ["@commitlint/config-conventional"],
  // History written before this lint existed cannot be reworded without rewriting shared commits.
  ignores: [(message) => message.startsWith("docs: AI_log & DECISIONS update")],
  rules: {
    "scope-enum": [
      2,
      "always",
      [
        "backend",
        "frontend",
        "scheduler",
        "plan",
        "ai",
        "api",
        "db",
        "ci",
        "docs",
        "deps",
      ],
    ],
    "header-max-length": [2, "always", 72],
  },
};
