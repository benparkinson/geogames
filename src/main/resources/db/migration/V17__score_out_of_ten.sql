-- Rounds are now scored out of 10 (a daily out of 50), not out of 100. Rescale the stored score
-- totals so past days stay roughly comparable. The buckets are tenths of the maximum: unchanged.
update daily_score_by_difficulty set score_total = (score_total + 5) / 10;
