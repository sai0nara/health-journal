/**
 * Wiki vault hooks for opencode.
 *
 * Runs the two tested wiki hooks at the end of each user turn
 * (on the "session.idle" bus event):
 *   Hook A  wiki/hooks/affected_pages.py  - names pages that cite changed code
 *   Hook B  wiki/hooks/report_health.py   - reports vault health from the lint
 *
 * The Python scripts do all the real work and are covered by
 * wiki/hooks/test_hooks.py, so this file stays a thin, defensive dispatcher:
 * every failure is swallowed so a hook can never break a session.
 *
 * Rules met here:
 *   * Hook A stays silent when only vault files changed (the scripts handle it).
 *   * Both scripts always exit 0 or mirror the lint's exit code; the plugin
 *     never throws.
 */

export const WikiVaultPlugin = async ({ project, directory, worktree, client, $ }) => {
  const root = (project && project.directory) || directory || worktree || process.cwd()
  const repo = (project && project.worktree) || worktree || root

  const runHook = async (script) => {
    try {
      const out = await $`python3 ${script} ${repo}`.nothrow().quiet().text()
      return out
    } catch (err) {
      return ""
    }
  }

  return {
    event: async ({ event }) => {
      if (!event || event.type !== "session.idle") return
      try {
        const affected = await runHook("wiki/hooks/affected_pages.py")
        if (affected && affected.trim()) {
          await client.app.log({
            body: {
              service: "wiki-vault",
              level: "info",
              message: "Wiki vault sync reminder:\n" + affected.trim(),
              extra: {},
            },
          })
        }

        const health = await runHook("wiki/hooks/report_health.py")
        if (health && health.trim()) {
          await client.app.log({
            body: {
              service: "wiki-vault",
              level: /exit=[1-9]/.test(health) ? "warn" : "info",
              message: "Wiki vault health:\n" + health.trim(),
              extra: {},
            },
          })
        }
      } catch (err) {
        // a failing hook must never break the session
      }
    },
  }
}