# Snowy — Robot Pet Project

Project-specific standards for snowy. Inherits shared standards from `../CLAUDE.md`.

---

## Workflow Overrides

Snowy uses the shared workflow from `../CLAUDE.md` with these overrides:

- **Term:** "project" instead of "plan" — use "project" in all workflow references (directory names, document text, status updates).
- **Directory:** `docs/projects/` instead of `docs/plans/`.
- **Numbering:** Run `docs/projects/next-number.sh` to get the next available number (do not manually scan directories).
- **Template extra field:** Add `**Related projects:** (list any prior completed projects this builds on or fixes)` after `**Created:**`.
- **Extra rule:** When fixing a bug or improving on a previously completed project, include a "Related projects" link. Check `docs/projects/completed/` for relevant prior work.
