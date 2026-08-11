# AI Agent Collaboration Guide

This project is optimized for collaboration between human developers and AI agents (GitHub Copilot, Manus, etc.). Follow these guidelines to ensure the "Magnifique" level of quality requested.

## Coding Standards for Agents
- **Language**: Use Kotlin 2.0+ with the latest Jetpack Compose features.
- **Architecture**: Strictly follow **Clean Architecture**. Logic must reside in UseCases, not in ViewModels.
- **Testing**: Every new feature MUST include a corresponding unit test in `app/src/test`.
- **Documentation**: Use KDoc for all public classes and methods.

## Task Delegation
When an AI agent is assigned a "Hard Task," it should:
1.  Read the corresponding specification in `docs/`.
2.  Propose a design pattern (e.g., Factory, Strategy) before implementation.
3.  Implement the feature in small, testable increments.

## Specific Agent Instructions for Eduk
- **App Blocking**: Focus on the `DevicePolicyManager` and `UsageStatsManager`. Avoid "overlay" hacks.
- **AI Vision**: When implementing the vision bridge, ensure high-quality error handling for blurry or low-light images.
- **Reporting**: Use a background service (WorkManager) to sync stats to the manager's account periodically.

---
*Agent Note: Let's build something extraordinary.*
