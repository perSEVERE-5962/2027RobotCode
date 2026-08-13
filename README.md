# FRC Team 5962 Robot Code for the 2026/2027 season

[![License](https://img.shields.io/badge/License-MIT-blue)](https://github.com/perSEVERE-5962/2027RobotCode/blob/main/LICENSE) ![Last Commit](https://img.shields.io/github/last-commit/perSEVERE-5962/2027RobotCode?color=yellow)

![Team 5962 perSEVERE](team_logo.svg)
---
## Branch Workflow
```text
feature/alice-arm ──────┐
                        │
feature/bob-shooter ────┼──► integration ──► main
                        │
feature/charlie-auto ───┘
```
Our repository follows a simple promotion model:

**Feature branches → `integration` → `main`**

Students develop features on their own branches created off of `integration` and submit pull requests to `integration`. After changes have been integrated and tested, `integration` is promoted to `main`.

`main` represents the code ready for competition

> **Do not merge feature branches directly into `main` or into other feature branches.**
---
📘 Our docs are proudly hosted with support from GitBook.
