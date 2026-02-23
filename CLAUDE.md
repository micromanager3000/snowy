# Project Best Practices

---

## Table of Contents

- [Workflow (READ FIRST)](#workflow-read-first)
- [Claude Code Efficiency](#claude-code-efficiency)

---

## Workflow (READ FIRST)

**Every task — no matter how small — goes through the plan lifecycle.** Bug fix, new feature, refactor, config change, docs update: all of them. No exceptions.

### The Process

1. **Draft a plan** — Claude writes a plan document to `docs/plans/drafts/`.
2. **Iterate** — User and Claude refine the plan. All prompts, responses, and decisions are captured in the document.
3. **Queue** — User approves. Plan moves to `in-queue/`.
4. **Implement** — Work begins. Plan moves to `in-progress/`. Progress is logged as work happens.
5. **Complete** — Implementation done, migrations applied. Plan moves to `completed/`.

### Directory Structure

```
docs/plans/
  drafts/        # Being refined with the user
  in-queue/      # Approved, ready to pick up
  in-progress/   # Currently being implemented
  completed/     # Done
```

### File Naming

```
NNN_kebab-case-name.md
```

- `NNN` — zero-padded 3-digit number, auto-incremented from the highest existing plan number across **all four directories**.
- Kebab-case for the name. Examples: `001_add-test-infrastructure.md`, `022_fix-websocket-reconnect.md`

### Plan Template

```markdown
# NNN: Plan Title

**Status:** Draft | In Queue | In Progress | Completed
**Created:** YYYY-MM-DD
**Project:** project-name

---

## Initial Prompt
> Paste the user's original request verbatim here.

## Background
Summary of key discussion points that shaped this plan — decisions made,
options explored, constraints identified. This captures conversational
context that would otherwise be lost.

## Context
Why this is needed — the problem, motivation, or opportunity.

## Approach
How we'll do it — phases, key decisions, trade-offs.

## Files Changed
Expected scope — new files, modified files.

## Open Questions
Anything unresolved. Remove this section once empty.

## Discussion Log
Chronological record of the planning conversation. Include user prompts,
Claude responses (summarized), and key decisions. Most recent at the bottom.

- **User:** [prompt text]
- **Claude:** [summary of response]
- **Decision:** [what was decided and why]

## Progress
Added when the plan moves to in-progress. Log completed steps, decisions
made during implementation, and remaining work. Most recent entries at top.

- YYYY-MM-DD: [What was done, any deviations from the plan, blockers hit]
```

### Lifecycle

| What happens | Claude does |
|---|---|
| User sends any task/request | Scan all dirs for highest plan number, write `NNN_name.md` to `drafts/` with status "Draft". Include the user's prompt verbatim in the Initial Prompt section. |
| User gives feedback / asks questions | Update the plan in `drafts/`, append to Discussion Log |
| User approves ("looks good" / "queue it" / "good to go") | Move to `in-queue/`, set status "In Queue" |
| User says to start / begins implementation | Move to `in-progress/`, set status "In Progress" |
| Implementation complete | Move to `completed/`, set status "Completed" |

### Rules

- **Always start in `drafts/`.** Never skip to `in-queue/` or `in-progress/`.
- **Capture everything.** The Initial Prompt section gets the user's exact words. The Discussion Log captures the back-and-forth. The Background section synthesizes it all into context.
- **Self-contained plans.** When a plan moves to `in-progress/`, implementation happens in a fresh conversation. The plan document + CLAUDE.md must be sufficient — no reliance on prior chat history.
- **Plan is source of truth.** Keep it updated as scope changes during implementation.
- **Progress tracking.** While implementing, maintain the Progress section. Log each meaningful step as it's completed. Most recent entries at top. Update as you work, not just at the end.
- **Listing plans.** When asked, scan all four directories and report by status.

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
