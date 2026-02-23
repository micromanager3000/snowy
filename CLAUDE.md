# Snowy — Robot Pet Project

---

## Table of Contents

- [Project Workflow (READ FIRST)](#project-workflow-read-first)
- [Claude Code Efficiency](#claude-code-efficiency)

---

## Project Workflow (READ FIRST)

**ALWAYS use the project workflow for every task unless the user explicitly asks you not to.** Bug fix, new feature, refactor, config change, docs update — everything gets a project. No exceptions.

### The Process

1. **Draft a project** — Claude writes a project document to `docs/projects/drafts/`.
2. **Iterate** — User and Claude refine the project. All prompts, responses, and decisions are captured in the document.
3. **Queue** — User approves. Project moves to `in-queue/`.
4. **Implement** — Work begins. Project moves to `in-progress/`. Progress is logged as work happens.
5. **Complete** — Implementation done. Project moves to `completed/`.

### Directory Structure

```
docs/projects/
  drafts/        # Being refined with the user
  in-queue/      # Approved, ready to pick up
  in-progress/   # Currently being implemented
  completed/     # Done
```

### File Naming

```
NNN_kebab-case-name.md
```

- `NNN` — zero-padded 3-digit number. Run `docs/projects/next-number.sh` to get the next available number.
- Kebab-case for the name. Examples: `001_android-phone-mvp.md`, `022_fix-deploy-script.md`

### Project Template

```markdown
# NNN: Project Title

**Status:** Draft | In Queue | In Progress | Completed
**Created:** YYYY-MM-DD
**Related projects:** (list any prior completed projects this builds on or fixes, e.g. "Builds on 002_zeroclaw-on-android.md")

---

## Initial Prompt
> Paste the user's original request verbatim here.

## Background
Summary of key discussion points that shaped this project — decisions made,
options explored, constraints identified.

## Context
Why this is needed — the problem, motivation, or opportunity.

## Approach
How we'll do it — phases, key decisions, trade-offs.

## Files Changed
Expected scope — new files, modified files.

## Open Questions
Anything unresolved. Remove this section once empty.

## Discussion Log
Chronological record of the planning conversation.

- **User:** [prompt text]
- **Claude:** [summary of response]
- **Decision:** [what was decided and why]

## Progress
Added when the project moves to in-progress. Most recent entries at top.

- YYYY-MM-DD: [What was done, any deviations from the project, blockers hit]
```

### Lifecycle

| What happens | Claude does |
|---|---|
| User sends any task/request | Run `docs/projects/next-number.sh` to get the next number, write `NNN_name.md` to `docs/projects/drafts/` with status "Draft". Include the user's prompt verbatim in the Initial Prompt section. |
| User gives feedback / asks questions | Update the project in `drafts/`, append to Discussion Log |
| User approves ("looks good" / "queue it" / "good to go") | Move to `in-queue/`, set status "In Queue" |
| User says to start / begins implementation | Move to `in-progress/`, set status "In Progress" |
| Implementation complete | Move to `completed/`, set status "Completed" |

### Rules

- **Always use this workflow.** Do not skip the project workflow unless the user explicitly tells you to. Even small bug fixes get a project.
- **Always start in `drafts/`.** Never skip to `in-queue/` or `in-progress/`.
- **Reference prior work.** When fixing a bug or improving on a previously completed project, include a "Related projects" link in the new project document. Check `docs/projects/completed/` for relevant prior work.
- **Capture everything.** The Initial Prompt section gets the user's exact words. The Discussion Log captures the back-and-forth. The Background section synthesizes it all into context.
- **Self-contained projects.** When a project moves to `in-progress/`, implementation happens in a fresh conversation. The project document + CLAUDE.md must be sufficient — no reliance on prior chat history.
- **Project is source of truth.** Keep it updated as scope changes during implementation.
- **Progress tracking.** While implementing, maintain the Progress section. Log each meaningful step as it's completed. Most recent entries at top.
- **Listing projects.** When asked, scan `docs/projects/` subdirs and report by status.

---

## Claude Code Efficiency

Practices to reduce token usage, cost, and latency when working in Claude Code.

### Sub-Agent Model Selection

Use the `model` parameter on Task tool calls to pick the cheapest model that can handle the job:

| Model | Use for | Examples |
|-------|---------|---------|
| **Haiku** | Simple, mechanical tasks | File searches, grep-and-report, listing files, simple code reads, formatting checks |
| **Sonnet** | Moderate reasoning | Code review, test writing, refactoring a single file, summarization |
| **Opus** | Complex reasoning (default) | Architecture decisions, multi-file refactors, debugging subtle issues |

**Default to Haiku for sub-agents** unless the task genuinely requires deeper reasoning. Most exploration and search tasks are Haiku-appropriate.

### Parallelization

- Launch independent sub-agents in a single message (parallel tool calls).
- Run independent Bash commands, Glob/Grep searches, and file reads in parallel.
- Only serialize when there's a true data dependency.

### Context Management

- Use sub-agents (Task tool) to offload large searches — keeps the main conversation context clean.
- For large files, use `offset` + `limit` on Read to fetch only the relevant section.
- Prefer Grep with `head_limit` over unbounded searches.

### General

- Read before editing — avoids failed edits and wasted turns.
- Use `Glob` and `Grep` directly for simple lookups; reserve `Explore` agents for open-ended investigation.
- Batch related edits into fewer Edit calls when possible.
