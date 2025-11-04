# AI Feature: Smart Study Coach

This feature suggests **what subject to study next** and **for how long**, based on your recent activity and focus performance. It runs fully on-device and updates in real time as you add or edit sessions.

## How it works
The Smart Study Coach computes a recommendation score for each subject using these signals (last 7 days):

1. Focus recovery need (higher priority):
   - Subjects with **lower average focus** (1–5) get a higher need, assuming they may benefit from a structured recovery session.
2. Recency penalty:
   - Subjects **not studied recently** receive a boost (to maintain subject balance).
3. Time allocation balancing:
   - Subjects with **less total minutes** get a small boost to prevent neglect.

Score formula (normalized):
score = w1 * (5 - avgFocus) + w2 * recencyDays + w3 * (minutesGapRatio)

Weights (default): w1=0.5, w2=0.3, w3=0.2

The recommended duration defaults to **45 minutes** and adapts based on your past average per session (bounded between 25 and 60 minutes).

## What you see
- A banner on the Home screen showing:
  - “Suggested next: {subjectName} — {duration} min”
  - Reason: “Low focus recently” / “Not studied in X days” / “Balance time”

## Privacy
- Runs offline using your local Room database and cached subject metadata.
- No data leaves your device.

## Limitations
- Initial recommendations may be generic until you have at least **3 sessions** across **2+ subjects**.
- Assumes consistent note quality and focus rating use.
